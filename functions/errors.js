/**
 * Módulo de clasificación de errores para Cloud Functions
 * Proporciona tipos de error y funciones para formatear respuestas consistentes
 */

const ErrorTypes = {
  FIRESTORE_ERROR: 'FIRESTORE_ERROR',
  TIMEOUT_ERROR: 'TIMEOUT_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  AUTH_ERROR: 'AUTH_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR'
};

/**
 * Clasifica un error y devuelve información sobre el tipo, código HTTP y si es reintentable
 * @param {Error} err - El error a clasificar
 * @returns {{type: string, status: number, retryable: boolean}}
 */
function classifyError(err) {
  const message = (err.message || String(err)).toLowerCase();

  if (message.includes('deadline_exceeded') || message.includes('timeout')) {
    return { type: ErrorTypes.TIMEOUT_ERROR, status: 504, retryable: true };
  }

  if (message.includes('permission_denied') || message.includes('unauthenticated')) {
    return { type: ErrorTypes.AUTH_ERROR, status: 403, retryable: false };
  }

  if (message.includes('not_found') || message.includes('invalid_argument')) {
    return { type: ErrorTypes.VALIDATION_ERROR, status: 400, retryable: false };
  }

  if (err.code && (err.code.startsWith('firestore/') || err.code.startsWith('storage/'))) {
    return { type: ErrorTypes.FIRESTORE_ERROR, status: 500, retryable: true };
  }

  if (message.includes('resource_exhausted') || message.includes('quota')) {
    return { type: ErrorTypes.FIRESTORE_ERROR, status: 429, retryable: true };
  }

  return { type: ErrorTypes.UNKNOWN_ERROR, status: 500, retryable: false };
}

/**
 * Formatea una respuesta de error consistente
 * @param {Error} err - El error original
 * @param {string} requestId - ID único de la petición para trazabilidad
 * @returns {{error: string, message: string, retryable: boolean, requestId: string, timestamp: string}}
 */
function formatErrorResponse(err, requestId) {
  const classified = classifyError(err);
  return {
    error: classified.type,
    message: err.message || String(err),
    retryable: classified.retryable,
    requestId: requestId,
    timestamp: new Date().toISOString()
  };
}

module.exports = {
  ErrorTypes,
  classifyError,
  formatErrorResponse
};
