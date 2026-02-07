# Security Setup Instructions

## Initial Setup

Before running the application, you need to configure your credentials:

### 1. Environment Variables Setup

Copy the example environment file and configure with your credentials:

```bash
cp .env.example .env
```

Edit `.env` and replace the placeholder values with your actual credentials:
- `MYSQL_ROOT_PASSWORD`: MySQL root password
- `MYSQL_USER`: Database username
- `MYSQL_PASSWORD`: Database user password
- `SPRING_DATASOURCE_USERNAME`: Same as MYSQL_USER
- `SPRING_DATASOURCE_PASSWORD`: Same as MYSQL_PASSWORD

### 2. Application Properties Setup (Optional)

For local development without Docker:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` and set your database credentials, or use environment variables.

## Running the Application

### With Docker Compose (Recommended)

Docker Compose will automatically load environment variables from `.env`:

```bash
docker-compose up -d
```

### Local Development

Set environment variables before running:

```bash
export SPRING_DATASOURCE_USERNAME=your_user
export SPRING_DATASOURCE_PASSWORD=your_password
./gradlew bootRun
```

## Security Notes

⚠️ **IMPORTANT**:
- Never commit `.env` file to version control
- Never commit `application.properties` with real credentials
- The `.env` file is already in `.gitignore`
- Use `.env.example` and `application.properties.example` as templates only
- Change default passwords before deploying to production

## Files to Commit vs Ignore

✅ **Safe to commit:**
- `.env.example`
- `application.properties.example`
- `.gitignore`

❌ **Never commit:**
- `.env`
- `application.properties` (if it contains real credentials)
- Any files with actual passwords or API keys
