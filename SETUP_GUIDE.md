# SafeGate Setup Guide

Complete guide for installing, configuring, and running SafeGate LLM Test Harness.

## Table of Contents

- [System Requirements](#system-requirements)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running SafeGate](#running-safegate)
- [GPU Acceleration](#gpu-acceleration)
- [Troubleshooting](#troubleshooting)
- [Advanced Configuration](#advanced-configuration)

---

## System Requirements

### Minimum Requirements (CPU Mode)

- **OS**: Windows 10/11, macOS 10.15+, or Linux (Ubuntu 20.04+)
- **RAM**: 8 GB (16 GB recommended)
- **Disk Space**: 20 GB free (for Docker images and one LLM model)
- **CPU**: 4 cores (8 cores recommended)
- **Docker**: Docker Desktop 4.0+ or Docker Engine 20.10+
- **Docker Compose**: v2.0+

### Recommended for GPU Acceleration

- **GPU**: NVIDIA GPU with 4+ GB VRAM (RTX 3060 Ti or better)
- **RAM**: 16 GB
- **Disk Space**: 30 GB free (for multiple LLM models)
- **NVIDIA Driver**: Latest version
- **CUDA**: 11.0+ (included in NVIDIA Container Toolkit)

---

## Installation

### Step 1: Install Docker

#### Windows

1. Download [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
2. Run the installer and follow prompts
3. Restart your computer
4. Verify installation:
   ```powershell
   docker --version
   docker compose version
   ```

#### macOS

1. Download [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)
2. Drag Docker.app to Applications
3. Launch Docker from Applications
4. Verify installation:
   ```bash
   docker --version
   docker compose version
   ```

#### Linux (Ubuntu/Debian)

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add your user to docker group
sudo usermod -aG docker $USER

# Logout and login again, then verify
docker --version
docker compose version
```

### Step 2: Clone SafeGate

```bash
git clone https://github.com/yourusername/safegate2.0.git
cd SafeGate
```

### Step 3: Start SafeGate

```bash
docker compose up -d
```

**What happens during first startup:**
1. Docker downloads required images (~2 GB total)
2. MySQL database initializes
3. Spring Boot application starts
4. Ollama service starts and downloads tinyllama model (~600 MB)
5. Python analyzer service starts

**Total time**: 5-10 minutes depending on internet speed

### Step 4: Verify Installation

```bash
# Check all containers are running
docker compose ps

# Should show 5 containers:
# - safegate-api (Spring Boot)
# - safegate-mysql (MySQL)
# - safegate-analyzer (Python)
# - ollama (Ollama LLM runtime)
# - ollama-init (Model downloader - will exit after completing)
```

### Step 5: Access SafeGate

Open your browser and navigate to:
- **Main Interface**: http://localhost:8080
- **LLM Configuration**: http://localhost:8080/llm.html
- **Dataset Testing**: http://localhost:8080/testing.html
- **Test Results**: http://localhost:8080/logs.html

---

## Configuration

### LLM Configuration

1. Go to http://localhost:8080/llm.html
2. Configure the following:

   **LLM Mode**:
   
   - `TEST_ONLY` - Analyze only dataset tests
  

   **Provider**: `ollama` (default and only option currently)

   **Model**: Choose from dropdown
   - `tinyllama` - Pre-downloaded, fastest
   - `phi3:mini` - Better accuracy
   - `mistral` - High accuracy
   - `llama3.2:3b-instruct` - Best accuracy

   **Analyzer URL**: `http://analyzer:5000/analyze` (default, don't change)

   **GPU Acceleration**: Check if using GPU mode (see GPU section below)

3. Click "Save Configuration"

### Pulling Additional Models

1. In LLM Configuration page, find "Pull arbitrary model" section
2. Enter model name (e.g., `phi3:mini`, `mistral`, `llama3.2:3b-instruct`)
3. Click "Pull Model"
4. Monitor progress bar (can take 5-30 minutes depending on model size)

**Model Sizes**:
- tinyllama: ~600 MB
- phi: ~2.7 GB
- phi3:mini: ~2.3 GB
- mistral: ~4.1 GB
- llama2: ~3.8 GB
- llama3.2:3b-instruct: ~2 GB

---

## Running SafeGate

### Starting SafeGate

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f

# View logs for specific service
docker compose logs -f api
docker compose logs -f analyzer
docker compose logs -f ollama
```

### Stopping SafeGate

```bash
# Stop all services (data is preserved)
docker compose down

# Stop and remove all data (WARNING: deletes all test results and models)
docker compose down -v
```

### Restarting SafeGate

```bash
docker compose restart

# Or restart specific service
docker compose restart api
```

### Updating SafeGate

```bash
# Pull latest changes
git pull

# Rebuild and restart
docker compose up -d --build
```

---

## GPU Acceleration

### Prerequisites

#### Windows with WSL2

1. Install [NVIDIA GPU Driver](https://www.nvidia.com/Download/index.aspx) (latest version)
2. Install [NVIDIA Container Toolkit for WSL2](https://docs.nvidia.com/cuda/wsl-user-guide/index.html)
3. Restart Docker Desktop

#### Linux

```bash
# Install NVIDIA Container Toolkit
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list

sudo apt-get update
sudo apt-get install -y nvidia-container-toolkit

# Configure Docker
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker

# Verify
nvidia-smi
```

### Enable GPU Mode

```bash
# Stop current stack
docker compose down

# Start with GPU profile
docker compose --profile gpu up -d
```

### Configure GPU in UI

1. Go to http://localhost:8080/llm.html
2. Check "Use GPU acceleration (CUDA)"
3. Click "Save Configuration"
4. Check status banner shows "GPU: On"

### Verify GPU is Working

```bash
# Check GPU is visible inside Ollama container
docker exec ollama nvidia-smi

# Should show your GPU and memory usage
```

### Performance Comparison

| Model | CPU (8 cores) | GPU (RTX 3060 Ti) |
|-------|---------------|-------------------|
| tinyllama | ~5 req/sec | ~20 req/sec |
| phi3:mini | ~2 req/sec | ~10 req/sec |
| mistral | ~1 req/sec | ~5 req/sec |

*Speeds are approximate and depend on hardware and payload complexity

---

## Troubleshooting

### Port Already in Use

**Problem**: Error binding to port 8080

**Solution**:
```bash
# Find process using port 8080
# Windows
netstat -ano | findstr :8080

# macOS/Linux
lsof -i :8080

# Kill the process or change SafeGate port
# Edit docker-compose.yml, change ports: "8081:8080"
```

### Models Download on Every Restart

**Problem**: tinyllama downloads every time

**Solution**: This should NOT happen with the current configuration. If it does:
```bash
# Check if ollama_data volume exists
docker volume ls | grep ollama

# If missing, recreate it
docker volume create ollama_data

# Restart
docker compose down
docker compose up -d
```

### Out of Disk Space

**Problem**: Docker running out of space

**Solution**:
```bash
# Check Docker disk usage
docker system df

# Remove unused data
docker system prune -a

# Remove old models (inside ollama container)
docker exec -it ollama ollama list
docker exec -it ollama ollama rm <model-name>
```

### Database Connection Errors

**Problem**: Application can't connect to MySQL

**Solution**:
```bash
# Check if MySQL is running
docker compose ps mysql

# Check MySQL logs
docker compose logs mysql

# Restart MySQL
docker compose restart mysql

# If persistent, reset database (WARNING: deletes all data)
docker compose down -v
docker compose up -d
```

### Analyzer Service Not Reachable

**Problem**: "Analyzer reachable: Unknown" in status banner

**Solution**:
```bash
# Check analyzer logs
docker compose logs analyzer

# Check if analyzer is running
docker compose ps analyzer

# Restart analyzer
docker compose restart analyzer

# Test manually
curl http://localhost:5001/models
```

### Model Pull Stuck

**Problem**: Model download never completes

**Solution**:
```bash
# Check Ollama logs
docker compose logs ollama

# Try pulling manually
docker exec -it ollama ollama pull tinyllama

# If stuck, restart Ollama
docker compose restart ollama
```

### GPU Not Detected

**Problem**: GPU: Off even after enabling

**Solutions**:
1. Verify NVIDIA Container Toolkit:
   ```bash
   nvidia-ctk --version
   ```

2. Check Docker can see GPU:
   ```bash
   docker run --rm --gpus all nvidia/cuda:11.0-base nvidia-smi
   ```

3. Verify GPU profile is active:
   ```bash
   docker compose ps
   # Should show ollama with "gpu" profile
   ```

4. Check Ollama container:
   ```bash
   docker exec ollama nvidia-smi
   ```

---

## Advanced Configuration

### Custom Ports

Edit `docker-compose.yml`:

```yaml
services:
  api:
    ports:
      - "8081:8080"  # Change 8081 to your desired port

  mysql:
    ports:
      - "3308:3306"  # Change 3308 to your desired port
```

### Custom Database Credentials

Edit `docker-compose.yml`:

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: your_root_password
      MYSQL_DATABASE: safegate_db
      MYSQL_USER: your_username
      MYSQL_PASSWORD: your_password

  api:
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/safegate_db
      SPRING_DATASOURCE_USERNAME: your_username
      SPRING_DATASOURCE_PASSWORD: your_password
```

### Resource Limits

Edit `docker-compose.yml` to limit resources:

```yaml
services:
  ollama:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G
```

### Persistent Logs

Add volume for application logs:

```yaml
services:
  api:
    volumes:
      - ./logs:/app/logs
```

### Environment Variables

Create `.env` file in project root:

```env
# MySQL
MYSQL_ROOT_PASSWORD=rootpass
MYSQL_DATABASE=safegate_db
MYSQL_USER=safegate_user
MYSQL_PASSWORD=SafeGate123!

# Application
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# LLM
OLLAMA_MODEL=tinyllama
```

Then reference in `docker-compose.yml`:

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
```

---

## Next Steps

- **[DATASET_GUIDE.md](DATASET_GUIDE.md)** - Learn how to prepare and test datasets
- **[README.md](README.md)** - Return to main documentation

---

**Need Help?**
- Open an issue: https://github.com/yourusername/SafeGate/issues
- Join discussions: https://github.com/yourusername/SafeGate/discussions
