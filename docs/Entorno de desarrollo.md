# Entorno de desarrollo - CRM Odontológico
# Copia este archivo a .env y ajusta los valores.

SPRING_PROFILES_ACTIVE=dev

# --- Base de datos ---
DATABASE_NAME=dentalcrm
DATABASE_USERNAME=dentalcrm
DATABASE_PASSWORD=your_database_password

# --- Seguridad ---
# Mínimo 32 caracteres aleatorios. Genera con: openssl rand -base64 48
JWT_SECRET=your_jwt_secret
JWT_EXPIRATION_MS=86400000

# --- Zona horaria ---
TIMEZONE=America/Guayaquil

# --- Puertos ---
FRONTEND_PORT=80

# --- CORS ---
CORS_ALLOWED_ORIGINS=http://localhost

# --- OpenWA ---
OPENWA_URL=http://openwa:2785
OPENWA_API_KEY=your_openwa_api_key

# --- Datos de la clínica (variables de plantillas) ---
CLINICA_NOMBRE=Clínica Dental Sonrisa Perfecta
CLINICA_DIRECCION=Av. Principal 123
CLINICA_TELEFONO=+593 99 123 4567

# --- Google Calendar ---
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI=http://localhost:8080/api/v1/google/callback
# URLs internas (por defecto apuntan a Google; se pueden sobreescribir para entorno de pruebas)
GOOGLE_AUTH_BASE_URL=https://accounts.google.com
GOOGLE_OAUTH_BASE_URL=https://oauth2.googleapis.com
GOOGLE_API_BASE_URL=https://www.googleapis.com
# --- Webhooks OpenWA -> backend ---
OPENWA_WEBHOOK_URL=http://backend:8080/api/v1/webhooks/whatsapp
OPENWA_WEBHOOK_SECRET=your_webhook_secret

