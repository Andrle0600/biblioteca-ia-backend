# 📚 Biblioteca IA Backend

Backend de gestión de biblioteca con **chatbot de recomendación de libros potenciado por IA** (Google Gemini + búsqueda vectorial con pgvector).

## 🚀 Tecnologías

- **Java 17** y **Spring Boot 4.1.1**
- **PostgreSQL** (AWS RDS) con la extensión **pgvector** para búsqueda por similitud de vectores
- **Spring Security + JWT** para autenticación stateless
- **Google Gemini** — `gemini-3.8-flash` (chat) y `gemini-embedding-001` (embeddings de 1536 dimensiones)
- **Lombok** y **Maven** (wrapper `mvnw` incluido)

## 📋 Requisitos previos

- JDK 17 o superior
- Maven (o usa el wrapper `mvnw` / `mvnw.cmd` incluido, no requiere instalación)
- Acceso a la base de datos PostgreSQL (AWS RDS)
- API key de Google Gemini — se consigue gratis en https://aistudio.google.com/apikey

## ⚙️ Configuración

Toda la configuración vive en `src/main/resources/application.properties` y se puede sobrescribir con variables de entorno:

| Variable de entorno | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de PostgreSQL | RDS de AWS (ver `application.properties`) |
| `DB_USERNAME` | Usuario de la BD | `postgres` |
| `DB_PASSWORD` | Contraseña de la BD | (ver `application.properties`) |
| `GEMINI_API_KEY` | **API key de Gemini (requerida para el chatbot)** | vacía |
| `GEMINI_CHAT_MODEL` | Modelo de chat | `gemini-3.8-flash` |
| `GEMINI_EMBEDDING_MODEL` | Modelo de embeddings | `gemini-embedding-001` |
| `GEMINI_EMBEDDING_DIMENSIONS` | Dimensiones del embedding | `1536` |
| `GEMINI_THINKING_BUDGET` | Tokens de razonamiento del modelo (0 = desactivado) | `0` |
| `GEMINI_REINTENTOS` | Reintentos ante 503/429 de la API | `3` |
| `JWT_SECRET` | Secreto para firmar tokens JWT | (ver `application.properties`) |
| `JWT_EXPIRATION` | Duración del token en ms | `86400000` (24 h) |

### 🔑 API key de Gemini

El chatbot necesita una API key de Google Gemini. Se consigue **gratis** en https://aistudio.google.com/apikey

**La key NO se sube al repositorio.** Colócala en el archivo **`.env`** en la **raíz del proyecto** (junto al `pom.xml`). Ese archivo está ignorado por git y la aplicación lo carga automáticamente al iniciar:

```env
GEMINI_API_KEY=tu_api_key_de_gemini_aqui
```

> 💡 Puedes usar `.env.example` como plantilla: copia el archivo como `.env` y completa los valores.
>
> ⚠️ **Importante**: nunca escribas la key en `application.properties` ni la subas a git. Si por accidente se expone, regenera la key en [Google AI Studio](https://aistudio.google.com/apikey).

## 🛠️ Pasos para ejecutar

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd biblioteca-ia-backend
```

### 2. Compilar

```bash
# Windows (PowerShell)
.\mvnw.cmd compile

# Linux / macOS
./mvnw compile
```

### 3. Ejecutar las pruebas

```bash
.\mvnw.cmd test
```

> Las pruebas necesitan que la base de datos esté accesible (el test `contextLoads` levanta el contexto completo de Spring).

### 4. Iniciar la aplicación

> Si ya creaste tu `.env` con la API key (paso anterior), no necesitas exportar nada: la app la carga sola. Solo ejecuta:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

**Alternativa sin `.env`** — pasar la key como variable de entorno solo para esa ejecución:

```powershell
# Windows (PowerShell)
$env:GEMINI_API_KEY="tu_api_key_de_gemini_aqui"
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS
export GEMINI_API_KEY="tu_api_key_de_gemini_aqui"
./mvnw spring-boot:run
```

La aplicación queda disponible en **http://localhost:8080**.

### 5. Verificar que está viva

```bash
curl http://localhost:8080/api/libros
```

## 📡 Endpoints principales

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/registrar` | Público | Registro de usuario |
| POST | `/api/auth/login` | Público | Login — devuelve JWT |
| GET | `/api/libros` | Público | Catálogo de libros |
| GET | `/api/libros/filtrar?titulo=&editorial=&genero=&orden=` | Público | Filtrar catálogo |
| GET | `/api/libros/{id}/ejemplares/disponibles` | Público | Ejemplares disponibles de un libro |
| GET | `/api/ejemplares/disponibles` | Público | Ejemplares disponibles |
| GET | `/api/ubicaciones` | Público | Ubicaciones físicas |
| POST/PUT/DELETE | `/api/libros/**` | ADMIN | CRUD de libros |
| POST/PUT/DELETE | `/api/ejemplares/**` | ADMIN | CRUD de ejemplares |
| POST | `/api/reservas/crear?usuarioId=&ejemplarId=&fechaRecojo=` | Autenticado | Crear reserva |
| GET | `/api/reservas` | ADMIN / BIBLIOTECARIO | Listar reservas |
| GET | `/api/reservas/pendientes` | ADMIN / BIBLIOTECARIO | Reservas pendientes |
| PUT | `/api/reservas/{id}` | ADMIN / BIBLIOTECARIO | Registrar devolución |
| GET | `/api/usuarios` | ADMIN / BIBLIOTECARIO | Listar usuarios |
| PUT | `/api/usuarios/{id}/activar` \| `/desactivar` | ADMIN / BIBLIOTECARIO | Activar/desactivar usuario |
| **POST** | **`/api/chat/mensaje`** | **Autenticado** | **💬 Chatear con el bot de recomendaciones** |
| GET | `/api/chat/historial?limite=30` | Autenticado | Historial de conversación |
| POST | `/api/chat/embeddings/regenerar` | ADMIN | Regenerar embeddings del catálogo |

### Ejemplo: conversar con el chatbot

```bash
# 1. Login para obtener el token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tu_correo@ejemplo.com","password":"tu_password"}'

# 2. Enviar un mensaje al chatbot (usa el token del paso anterior)
curl -X POST http://localhost:8080/api/chat/mensaje \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"mensaje":"Recomiendame un libro que me haga reflexionar"}'
```

Respuesta:

```json
{
  "respuesta": "Te recomiendo 'El Pez en el Agua' de Mario Vargas Llosa...",
  "librosSugeridos": [
    { "id": 4, "titulo": "El Pez en el Agua", "autor": "Mario Vargas Llosa", "genero": "...", "similitud": 0.6412 }
  ]
}
```

## 🤖 Cómo funciona el chatbot de recomendaciones

1. El mensaje del usuario se convierte en **vector (embedding)** con `gemini-embedding-001`.
2. El vector del mensaje se **combina con el perfil vectorial del usuario** (`perfil_vector_usuario`): el promedio de los embeddings de los libros que ha reservado. Peso configurable: 60% mensaje / 40% perfil.
3. Se buscan los **libros más similares por distancia coseno** en la tabla `libro_vector` (pgvector, índice HNSW), excluyendo los libros que ya leyó y priorizando los que tienen ejemplares **DISPONIBLES**.
4. `gemini-3.8-flash` genera la respuesta final en español, recomendando **únicamente** de entre los candidatos recuperados.
5. Toda la conversación se guarda en `chat_interaccion`, y el perfil vectorial se **actualiza automáticamente** al reservar o devolver un libro.

**Al agregar libros nuevos al catálogo**, ejecuta una vez (como ADMIN):

```bash
curl -X POST http://localhost:8080/api/chat/embeddings/regenerar -H "Authorization: Bearer <TOKEN_ADMIN>"
```

## 🗄️ Base de datos (PostgreSQL + pgvector)

La BD en uso es una instancia de **AWS RDS PostgreSQL** con la extensión `pgvector` instalada (ver `application.properties`).

Las tablas de JPA (`usuarios`, `role`, `user_role`, `libro`, `ejemplar`, `ubicacion`, `reserva`, `chat_interaccion`) se crean automáticamente por `spring.jpa.hibernate.ddl-auto=update`.

Las **tablas vectoriales** no las gestiona Hibernate; si montas la BD desde cero, ejecuta:

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS libro_vector (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    libro_id BIGINT NOT NULL UNIQUE REFERENCES libro(id) ON DELETE CASCADE,
    embedding vector(1536),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_libro_vector_embedding_hnsw
    ON libro_vector USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS perfil_vector_usuario (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    embedding vector(1536),
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_perfil_vector_usuario_embedding_hnsw
    ON perfil_vector_usuario USING hnsw (embedding vector_cosine_ops);
```

### PostgreSQL local alternativo (con Docker)

```bash
docker run -d --name biblioteca-postgres -p 5432:5432 \
  -e POSTGRES_DB=biblioteca -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=root \
  pgvector/pgvector:pg17
```

Y sobrescribe las variables: `DB_URL=jdbc:postgresql://localhost:5432/biblioteca`, `DB_USERNAME=postgres`, `DB_PASSWORD=root`.

## 📁 Estructura del proyecto

```
src/main/java/com/biblioteca/
├── BibliotecaApplication.java        # Clase principal
├── presentacion/                     # Capa de presentación
│   ├── config/                       # Configuración (CORS)
│   ├── dto/                          # Objetos de transferencia (request/response)
│   └── web/                          # Controladores REST (API)
├── negocio/                          # Capa de negocio
│   └── service/                      # AuthService, LibroService, ReservaLibroService,
│                                      # ChatbotService, GeminiService, PerfilVectorService...
├── persistencia/                     # Capa de persistencia
│   ├── entity/                       # Entidades JPA
│   ├── repository/                   # Repositorios Spring Data JPA
│   └── vector/                       # Acceso a tablas pgvector (JdbcTemplate)
└── seguridad/                        # JWT, SecurityConfig, UserDetailsService
```

## 👤 Usuario de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `chattest@biblioteca.com` | `Chat1234!` | `ROLE_USER` |

## 🔒 Notas de seguridad

- La API key de Gemini **nunca se sube al repositorio**: vive en el archivo `.env` (raíz del proyecto, ignorado por git) o en una variable de entorno. Si por accidente se expone, regenera la key en [Google AI Studio](https://aistudio.google.com/apikey).
- Los tokens JWT expiran en 24 h por defecto (`JWT_EXPIRATION`).
- Las rutas de escritura (libros, ejemplares, usuarios, reservas) requieren roles `ADMIN` o `BIBLIOTECARIO`; el chat requiere usuario autenticado.



