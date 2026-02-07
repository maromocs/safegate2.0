# SafeGate - LLM Test Harness for Security Dataset Evaluation

SafeGate is a specialized test harness designed to evaluate Large Language Model (LLM) performance in detecting security threats and attack payloads. It allows security researchers and ML engineers to test different LLM models against various security datasets and compare their effectiveness.

## 🎯 What is SafeGate?

SafeGate transforms security datasets into actionable insights about LLM detection capabilities. Upload attack payloads in various formats, run them through your chosen LLM, and get detailed reports on what was detected and what was missed.

**Key Focus**: This is NOT a production WAF. It's a **testing and evaluation platform** for LLM-based security detection.

## ✨ Key Features

### 🤖 LLM Integration
- **Multiple Model Support**: Test with tinyllama, phi, phi3:mini, mistral, llama2, llama3.2:3b-instruct, or any Ollama model
- **Batch Processing**: Efficient batch analysis of payloads with detailed per-category statistics
- **GPU Acceleration**: Optional CUDA support for faster inference (NVIDIA GPUs)
- **Provider-Agnostic**: Built on Ollama but extensible to other providers

### 📊 Dataset Testing
- **Multi-Format Support**: CSV, JSON, XML, TXT, TSV with automatic format detection
- **Flexible Sampling**: Test all payloads or random samples (100, 1000, 10000, etc.)
- **Deterministic Testing**: Optional seed parameter for reproducible results
- **CSIC 2010 Ready**: Pre-configured for popular security dataset formats

### 📈 Results & Analysis
- **Comprehensive Reports**: View detection rates, category breakdowns, and passed payloads
- **Historical Tracking**: Keep all test runs for comparison and trend analysis
- **Detailed Logs**: Examine individual LLM decisions with reasoning
- **Export Capabilities**: Download safe and malicious payloads for further analysis

### 🎨 Modern Interface
- Beautiful dark-themed web UI with real-time updates
- Interactive result exploration with expandable details
- Model management (pull, configure, monitor)
- GPU status monitoring and configuration

## 🏗️ Architecture

```
┌─────────────────┐
│   Web Browser   │ (Modern Dark UI)
└────────┬────────┘
         │ HTTP
         ▼
┌─────────────────┐
│  Spring Boot    │ (Java 21 Backend)
│   API Server    │
└────────┬────────┘
         │
    ┌────┴────┬──────────┬─────────┐
    ▼         ▼          ▼         ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ MySQL  │ │Analyzer│ │ Ollama │ │ Docker │
│   DB   │ │Service │ │  LLM   │ │Volumes │
└────────┘ └────────┘ └────────┘ └────────┘
```

### Components

1. **Spring Boot API** - Core application handling requests, test orchestration, and data persistence
2. **Python Analyzer** - Microservice interfacing with Ollama for LLM inference
3. **Ollama Service** - LLM runtime (CPU or GPU mode)
4. **MySQL Database** - Stores test runs, results, and configuration
5. **Web Interface** - Modern responsive UI for interaction

## 🚀 Quick Start

### Prerequisites

- **Docker & Docker Compose** (required)
- **20+ GB disk space** (for LLM models)
- **Optional**: NVIDIA GPU with Container Toolkit for GPU acceleration

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/SafeGate.git
   cd SafeGate
   ```

2. **Start SafeGate (CPU mode)**
   ```bash
   docker compose up -d
   ```

3. **Access the interface**
   - Open http://localhost:8080
   - Wait for tinyllama model to download (~600 MB, first time only)

4. **Optional: GPU acceleration**
   ```bash
   docker compose --profile gpu up -d
   ```

### First Test Run

1. Go to http://localhost:8080/llm.html
2. Configure LLM settings (tinyllama is pre-configured)
3. Go to http://localhost:8080/testing.html
4. Upload a dataset (CSV, JSON, XML, TXT, TSV)
5. Click "Start Dataset Test"
6. View results in http://localhost:8080/logs.html

## 📖 Documentation

- **[SETUP_GUIDE.md](SETUP_GUIDE.md)** - Detailed installation, configuration, and troubleshooting
- **[DATASET_GUIDE.md](DATASET_GUIDE.md)** - Dataset formats, preparation, and examples
- **[API_DOCUMENTATION.md](API_DOCUMENTATION.md)** - REST API reference for automation

## 🎯 Use Cases

### Security Researchers
- Evaluate new LLM models for security threat detection
- Compare different models on the same dataset
- Identify blind spots in LLM-based detection

### ML Engineers
- Test prompt engineering improvements
- Benchmark model performance on security tasks
- Fine-tune models using passed payloads as training data

### Red Teams
- Discover bypass techniques for LLM-based WAFs
- Build evasion payload datasets
- Test payload obfuscation effectiveness

### Academic Research
- Reproducible experiments with deterministic sampling
- Comparative studies across models
- Dataset curation and validation

## 🔧 Technology Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 21, Spring Boot 3.5 |
| Frontend | HTML5, CSS3, JavaScript (Vanilla) |
| Database | MySQL 8.0 |
| LLM Runtime | Ollama |
| Containerization | Docker, Docker Compose |
| Build Tool | Gradle 8.14 |

## 📊 Supported Dataset Formats

- **CSV** - With "payload" column
- **JSON** - Array of strings or objects with payload field
- **XML** - Elements or attributes containing payloads
- **TXT** - One payload per line
- **TSV** - Tab-separated with "payload" column

See [DATASET_GUIDE.md](DATASET_GUIDE.md) for detailed format specifications.

## 🎮 Model Management

SafeGate automatically downloads **tinyllama** (~600 MB) on first startup. Additional models can be pulled through the UI:

- **tinyllama** - Fastest, smallest, good for testing
- **phi3:mini** - Balanced speed/accuracy
- **mistral** - High accuracy, larger model
- **llama3.2:3b-instruct** - Best accuracy, instruction-tuned

## 🐛 Troubleshooting

**Models downloading on every startup?**
- This is now fixed! Models persist in Docker volumes
- Only tinyllama downloads automatically (once)

**Out of disk space?**
- Each model is 600 MB - 4 GB
- Delete unused models via UI or `docker volume prune`

**GPU not detected?**
- Ensure NVIDIA Container Toolkit is installed
- Start with `docker compose --profile gpu up -d`
- Verify with `docker exec ollama nvidia-smi`

## 📝 License

This project is licensed under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit Pull Requests.



## ⭐ Star History

If you find SafeGate useful, please consider giving it a star!

---

**Last Updated**: February 2026
**Version**: 2.0 - LLM Test Harness Edition
