import { createContext, useContext, useState } from 'react'

const translations = {
  es: {
    // Login
    identifier:  'Usuario o correo',
    password:    'Contraseña',
    submit:      'Acceder',
    loading:     'Verificando...',
    systemLabel: 'SISTEMA DE AUTENTICACIÓN',
    tagline:     'Custodia segura de documentos de identidad',
    secureConn:  'CONEXIÓN SEGURA',
    required:    'Este campo es obligatorio.',

    // Dashboard
    myDocuments:       'Mis documentos',
    uploadDocument:    'Subir documento',
    noDocuments:       'No hay documentos registrados.',
    noDocumentsSub:    'Subí tu primer documento de identidad para comenzar.',
    loadingDocuments:  'Cargando documentos...',
    logout:            'Salir',
    page:              'Página',
    of:                'de',
    previous:          'Anterior',
    next:              'Siguiente',
    expires:           'Vence',
    uploaded:          'Subido',
    download:          'Descargar',
    processing:        'Procesando',
    processed:         'Procesado',
    failed:            'Fallido',
    unknownType:       'Tipo desconocido',
    typeLabels: {
      IDENTITY_CARD:  'DNI',
      PASSPORT:       'Pasaporte',
      DRIVER_LICENCE: 'Licencia',
      OTHER:          'Otro',
    },

    // Filters
    filterAll:     'Todos',
    filterExpired: 'Vencidos',

    // Expiration date edit
    editExpireAt:     'Editar fecha',
    saveExpireAt:     'Guardar',
    cancelEdit:       'Cancelar',
    savingExpireAt:   'Guardando...',
    editSuccess:      'Fecha actualizada.',

    // Upload modal
    uploadTitle:      'Subir documento',
    uploadSub:        'JPG, PNG o PDF · Máx. 10 MB',
    uploadDrop:       'Arrastrá un archivo o hacé clic para seleccionar',
    uploadSelected:   'Archivo seleccionado',
    uploading:        'Subiendo...',
    uploadConfirm:    'Subir',
    uploadCancel:     'Cancelar',
    uploadSuccess:    'Documento subido. El OCR comenzará en segundos.',

    errors: {
      USER_NOT_FOUND:             'Usuario no encontrado.',
      INVALID_CREDENTIALS:        'Credenciales incorrectas.',
      NETWORK_ERROR:              'Error de conexión. Intente nuevamente.',
      RATE_LIMIT_EXCEEDED:        'Límite de subidas diarias alcanzado.',
      DOCUMENT_PROCESSING_ERROR:  'No se pudo procesar el archivo.',
      INVALID_EXPIRATION_DATE:    'La fecha no puede ser anterior a hoy.',
      DEFAULT:                    'Error inesperado. Intente nuevamente.',
    },
  },

  en: {
    // Login
    identifier:  'Username or email',
    password:    'Password',
    submit:      'Access',
    loading:     'Verifying...',
    systemLabel: 'AUTHENTICATION SYSTEM',
    tagline:     'Secure custody of identity documents',
    secureConn:  'SECURE CONNECTION',
    required:    'This field is required.',

    // Dashboard
    myDocuments:       'My documents',
    uploadDocument:    'Upload document',
    noDocuments:       'No documents on record.',
    noDocumentsSub:    'Upload your first identity document to get started.',
    loadingDocuments:  'Loading documents...',
    logout:            'Logout',
    page:              'Page',
    of:                'of',
    previous:          'Previous',
    next:              'Next',
    expires:           'Expires',
    uploaded:          'Uploaded',
    download:          'Download',
    processing:        'Processing',
    processed:         'Processed',
    failed:            'Failed',
    unknownType:       'Unknown type',
    typeLabels: {
      IDENTITY_CARD:  'ID Card',
      PASSPORT:       'Passport',
      DRIVER_LICENCE: 'Driver\'s License',
      OTHER:          'Other',
    },

    // Filters
    filterAll:     'All',
    filterExpired: 'Expired',

    // Expiration date edit
    editExpireAt:     'Edit date',
    saveExpireAt:     'Save',
    cancelEdit:       'Cancel',
    savingExpireAt:   'Saving...',
    editSuccess:      'Date updated.',

    // Upload modal
    uploadTitle:      'Upload document',
    uploadSub:        'JPG, PNG or PDF · Max 10 MB',
    uploadDrop:       'Drag a file or click to select',
    uploadSelected:   'File selected',
    uploading:        'Uploading...',
    uploadConfirm:    'Upload',
    uploadCancel:     'Cancel',
    uploadSuccess:    'Document uploaded. OCR will start in seconds.',

    errors: {
      USER_NOT_FOUND:             'User not found.',
      INVALID_CREDENTIALS:        'Invalid credentials.',
      NETWORK_ERROR:              'Connection error. Please try again.',
      RATE_LIMIT_EXCEEDED:        'Daily upload limit reached.',
      DOCUMENT_PROCESSING_ERROR:  'Could not process the file.',
      INVALID_EXPIRATION_DATE:    'Date cannot be in the past.',
      DEFAULT:                    'Unexpected error. Please try again.',
    },
  },
}

const LanguageContext = createContext(null)

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState(
    () => localStorage.getItem('dosafe_lang') || 'es'
  )

  function toggle() {
    const next = lang === 'es' ? 'en' : 'es'
    setLang(next)
    localStorage.setItem('dosafe_lang', next)
  }

  return (
    <LanguageContext.Provider value={{ lang, toggle, t: translations[lang] }}>
      {children}
    </LanguageContext.Provider>
  )
}

export function useLanguage() {
  return useContext(LanguageContext)
}
