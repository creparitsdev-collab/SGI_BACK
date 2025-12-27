# Gestor-y-control-de-documentos-Back-end-
Back-end, logica para la aplicacion web para el gestor de documentos.

## Variables de entorno (Render)

Este backend usa el profile `prod` en Render. La configuración de producción se encuentra en `src/main/resources/application-prod.properties` y **lee valores desde variables de entorno**.

Configura estas variables en tu servicio de Render:

- **DATABASE_URL**
  Valor esperado: URL JDBC de Postgres.
  Ejemplo de formato:
  `jdbc:postgresql://<HOST>:5432/<DATABASE_NAME>`

- **DATABASE_USERNAME**
  Usuario de la base de datos.

- **DATABASE_PASSWORD**
  Contraseña de la base de datos.

- **JWT_SECRET**
  Secreto para firmar JWT.

- **JWT_EXPIRATION** (opcional)
  Milisegundos.
  Default: `3600000`

- **JWT_EXPIRATION_RECOVERY** (opcional)
  Milisegundos.
  Default: `1800000`

- **FRONTEND_URL**
  URL del frontend (se usa para CORS y para links en correos).

- **DDL_STRATEGY** (opcional)
  Estrategia Hibernate.
  Valores típicos: `create` (primer deploy), `update` (para conservar datos).
  Default: `create`

- **RESEND_API_KEY**
  API Key de Resend.

- **RESEND_DEFAULT_SENDER** (opcional)
  Remitente por defecto.
  Default: `onboarding@resend.dev`

- **RESEND_VERIFIED_DOMAINS** (opcional)
  Lista CSV de dominios verificados en Resend.
  Ejemplo: `creparisdev.site,otro-dominio.com`
  Si no se define, se permite el mismo dominio que el del `RESEND_DEFAULT_SENDER`.

Notas:

- Render inyecta automáticamente `PORT` y el backend lo usa en `server.port`.
- No guardes valores reales de credenciales en el repo (ni en `application-prod.properties` ni en archivos de ejemplo).

## Configuración de Resend

En Resend necesitas:

- **Dominio verificado** (por ejemplo `creparisdev.site`).
- **Sender** dentro de ese dominio (por ejemplo `noreply@creparisdev.site`).

En Render:

- Define `RESEND_DEFAULT_SENDER` con tu sender.
- Define `RESEND_VERIFIED_DOMAINS` con el/los dominios verificados.

