/**
 * Cloud Function (Node.js) para purgar notificaciones expiradas en batches.
 * Diseño pensado para escala: consulta por `expiresAt <= now`, borra en lotes
 * (max 400 por batch) y reitera hasta terminar. Se recomienda activar el
 * TTL nativo de Firestore si está disponible (índice TTL en el campo `expiresAt`).
 *
 * Despliegue:
 * - npm install
 * - gcloud functions deploy purgeExpiredNotifications --runtime=node20 --trigger-http --region=YOUR_REGION
 * - Programar con Cloud Scheduler para llamar esta función cada X horas.
 *
 * Nota: en producción, restringe la invocación con IAM y no uses --allow-unauthenticated
 */

const { onRequest } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require('firebase-admin');
const crypto = require('crypto');
const { classifyError, formatErrorResponse } = require('./errors');

admin.initializeApp();
const db = admin.firestore();

// Configuración global (región)
setGlobalOptions({ region: "us-central1" });

// Configuración
const BATCH_SIZE = 400;    // Límite de documentos por batch (max 500)
const MAX_BATCHES = 50;    // Límite de iteraciones para evitar loops infinitos

exports.purgeExpiredNotifications = onRequest(async (req, res) => {
  // Genero ID único para esta petición (para trazabilidad en logs)
  const requestId = crypto.randomUUID();
  const startTime = Date.now();

  // Detecto si viene de Cloud Scheduler para logging
  const userAgent = req.get('User-Agent') || '';
  const isScheduler = userAgent.includes('Google-Cloud-Scheduler');

  console.log(`[${requestId}] Iniciando purga de notificaciones. Fuente: ${isScheduler ? 'Cloud Scheduler' : 'Manual'}`);

  try {
    const now = Date.now();
    let totalDeleted = 0;
    let batchCount = 0;

    while (batchCount < MAX_BATCHES) {
      // Query por batch ordenado por expiresAt para paginación estable
      const snapshot = await db.collection('notificaciones')
        .where('expiresAt', '<=', now)
        .orderBy('expiresAt')
        .limit(BATCH_SIZE)
        .get();

      if (snapshot.empty) {
        console.log(`[${requestId}] No hay más documentos expirados`);
        break;
      }

      const batch = db.batch();
      snapshot.docs.forEach(doc => batch.delete(doc.ref));
      await batch.commit();

      totalDeleted += snapshot.size;
      batchCount++;

      console.log(`[${requestId}] Batch ${batchCount}: eliminados ${snapshot.size} documentos`);

      // Si menos que batch size, hemos terminado
      if (snapshot.size < BATCH_SIZE) break;
    }

    const duration = Date.now() - startTime;

    // Aviso si llegamos al límite de batches
    if (batchCount >= MAX_BATCHES) {
      console.warn(`[${requestId}] Se alcanzó el límite de ${MAX_BATCHES} batches. Puede haber más documentos por eliminar.`);
    }

    console.log(`[${requestId}] Purga completada. Eliminados: ${totalDeleted}, Batches: ${batchCount}, Duración: ${duration}ms`);

    res.status(200).send({
      deleted: totalDeleted,
      batches: batchCount,
      durationMs: duration,
      requestId: requestId,
      limitReached: batchCount >= MAX_BATCHES
    });

  } catch (err) {
    const duration = Date.now() - startTime;
    console.error(`[${requestId}] Error después de ${duration}ms:`, err);

    const classified = classifyError(err);
    const errorResponse = formatErrorResponse(err, requestId);

    res.status(classified.status).send(errorResponse);
  }
});
