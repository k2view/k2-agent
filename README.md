# K2View Agent 🚀

[![Java Version](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
## 📋 Table of Contents
- [Description](#-description)
- [Features](#-features)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Usage](#-usage)
- [Configuration](#-configuration)
- [Architecture](#-architecture)
- [Security](#-security)
- [Contributing](#-contributing)
- [Troubleshooting](#-troubleshooting)
- [License](#-license)
- [Contact](#-contact)

## 📝 Description

K2View Agent is a high-performance Java application designed for efficient handling of asynchronous HTTP requests and responses. It serves as a robust middleware solution for managing communication between K2View Cloud Orchestrator and the remote Kubernetes environment.

## ✨ Features

- 🔄 Asynchronous HTTP request handling
- 📊 Customizable request queuing with size limits
- 🔒 SSL support with configurable certificate validation
- 🎛️ Flexible response handling with timeout options
- 📈 Comprehensive logging of request and response activities
- 🌐 Dynamic environment variable support for URLs and headers
- 🧵 Multi-threaded processing for enhanced performance

## 🔧 Requirements

- Java 21 or higher
- Maven 3.6+ (for building the project)
- A compatible IDE (e.g., IntelliJ IDEA, VS Code, Eclipse) for development

## 📦 Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-org/k2view-agent.git
   ```

2. Navigate to the project directory:
   ```
   cd k2view-agent
   ```

3. Build the project:
   ```
   mvn clean package
   ```

This will create a JAR file in the `target` directory.

## 🚀 Usage

### Initializing the Agent

```java
AgentDispatcher dispatcher = new AgentDispatcherHttp(1000); // 1000 is the max queue size
```

### Sending a Request

```java
Map<String, Object> headers = new HashMap<>();
headers.put("Content-Type", "application/json");

Request request = new Request("task-123", "https://api.example.com/data", "POST", headers, "{\"key\":\"value\"}");
dispatcher.send(request);
```

### Receiving Responses

```java
List<Response> responses = dispatcher.receive(10, TimeUnit.SECONDS);
for (Response response : responses) {
    System.out.println("Task ID: " + response.taskId());
    System.out.println("Status Code: " + response.code());
    System.out.println("Body: " + response.body());
}
```

### Closing the Dispatcher

```java
dispatcher.close();
```

## ⚙️ Configuration

The application uses environment variables for configuration:

| Variable | Description | Default |
|----------|-------------|---------|
| `K2_MAILBOX_ID` | The mailbox ID for the agent | - |
| `K2_MANAGER_URL` | The URL of the manager service | - |
| `K2_POLLING_INTERVAL` | The polling interval in seconds | 10 |

Set these environment variables before running the application.

## 🏗️ Architecture

The K2View Agent is built on a modular architecture:

- `AgentDispatcher`: Core interface defining the contract for request dispatching.
- `AgentDispatcherHttp`: Implementation of `AgentDispatcher` using Java's `HttpClient`.
- `Request`: Represents an HTTP request with task ID, URL, method, headers, and body.
- `Response`: Encapsulates the HTTP response with task ID, status code, and body.

## 🔐 Security

### SSL Configuration

By default, the application trusts all SSL certificates. For production, implement proper certificate validation:

1. Modify `noCertificateCheckSSLContext()` in `AgentDispatcherHttp`.
2. Use a custom `TrustManager` that validates certificates against your trusted CA list.

### Best Practices

- Use HTTPS for all communications.
- Implement proper authentication mechanisms (e.g., API keys, OAuth).
- Regularly update dependencies to patch security vulnerabilities.

## 🤝 Contributing

We welcome contributions to K2View Agent! Here's how you can help:

1. Fork the repository.
2. Create a new branch: `git checkout -b feature/your-feature-name`.
3. Make your changes and commit: `git commit -m 'Add some feature'`.
4. Push to your fork: `git push origin feature/your-feature-name`.
5. Create a pull request.

Please ensure your code adheres to our coding standards and includes appropriate tests.

## 🔍 Troubleshooting

Common issues and their solutions:

- **Connection timeouts**: Check network settings and firewall configurations.
- **SSL errors**: Ensure proper SSL certificate validation is implemented.
- **Out of memory errors**: Adjust JVM heap size or review request queue size.

For more issues, please check our [FAQ](link-to-faq) or open an issue on GitHub.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

For support or queries, please contact:

- Email: support@k2view.com
 
---

Made with ❤️ by the K2View Team
