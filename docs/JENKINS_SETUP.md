# Jenkins CI/CD Pipeline Setup

This guide explains how to set up Jenkins for AgrawalPulse microservices.

## Prerequisites

- Jenkins server running (v2.401+)
- Java 21 installed on Jenkins agent
- Maven 3.9.0+ installed
- Node.js 20+ installed
- Docker installed (for building images)
- Git plugin installed in Jenkins

## Jenkins Plugins Required

Install these plugins in Jenkins → Manage Jenkins → Manage Plugins:

1. **Pipeline** - workflow engine
2. **Git** - Git integration
3. **Docker** - Docker plugin
4. **Maven Integration** - Maven build support
5. **NodeJS** - Node.js support
6. **Credentials** - Credential management
7. **Email Extension** - Email notifications
8. **Slack** (optional) - Slack notifications

## Setup Steps

### 1. Create Jenkins Credentials

#### Docker Registry Credentials
1. Go to **Manage Jenkins** → **Manage Credentials**
2. Click **Global credentials (unrestricted)**
3. Click **Add Credentials**
4. Select **Username with password**
5. Fill in:
   - **Username**: Your Docker Hub username (or registry username)
   - **Password**: Your Docker Hub password/token
   - **ID**: `docker-registry-credentials`
   - **Description**: Docker Registry Credentials
6. Click **Create**

#### GitHub SSH Key (Optional, for private repos)
1. Repeat steps 1-3 above
2. Select **SSH Username with private key**
3. Fill in your GitHub SSH key details
4. **ID**: `github-ssh-key`
5. Click **Create**

### 2. Create a Jenkins Pipeline Job

1. Click **New Item**
2. Enter job name: `AgrawalPulse-Pipeline`
3. Select **Pipeline**
4. Click **OK**

### 3. Configure Pipeline Job

#### General
- ☑ **Discard old builds**: Keep 10 most recent

#### Build Triggers
- ☑ **GitHub hook trigger for GITScm polling**
  - This allows GitHub webhooks to trigger builds on push
- Or ☑ **Poll SCM**: `H/15 * * * *` (check every 15 minutes)

#### Pipeline
- **Definition**: `Pipeline script from SCM`
- **SCM**: `Git`
  - **Repository URL**: `https://github.com/himanshu30243/AgrawalPulse.git`
  - **Credentials**: Select your GitHub credentials
  - **Branch Specifier**: `*/main`
  - **Script Path**: `Jenkinsfile`

4. Click **Save**

### 4. Set Up GitHub Webhook (Automatic Builds)

#### In GitHub
1. Go to your repository: https://github.com/himanshu30243/AgrawalPulse
2. Settings → Webhooks
3. Click **Add webhook**
4. **Payload URL**: `http://<your-jenkins-url>/github-webhook/`
   - Example: `http://192.168.1.100:8080/github-webhook/`
5. **Content type**: `application/json`
6. **Which events**: Select **Just the push event**
7. ☑ **Active**
8. Click **Add webhook**

#### In Jenkins
1. Go to job configuration
2. Check: **GitHub hook trigger for GITScm polling**
3. Save

Now every push to `main` will trigger a build!

### 5. Configure Environment Variables (Optional)

If you want to customize Docker registry or other settings:

1. Go to **Manage Jenkins** → **Configure System**
2. Under **Global properties**
3. Add environment variables:
   - `DOCKER_REGISTRY_URL`: `docker.io` or your registry
   - `DOCKER_IMAGE_PREFIX`: your Docker Hub username

### 6. Run First Build

1. Go to job: `AgrawalPulse-Pipeline`
2. Click **Build Now**
3. Wait for build to complete
4. Check console output for logs

## Pipeline Stages Explained

| Stage | What it does |
|-------|-------------|
| **Checkout** | Clones repo from GitHub |
| **Build Backend** | Runs `mvn clean install` on 6 services |
| **Build Frontend** | Runs `npm install && npm run build` |
| **Backend Tests** | Runs `mvn test` on all services |
| **Frontend Tests** | Runs `npm test` if configured |
| **Code Quality** | Runs SpotBugs static analysis |
| **Build Docker Images** | Creates Docker images for all 7 services (6 backend + 1 frontend) |
| **Push to Registry** | Pushes images to Docker Hub or registry |
| **Deploy to Dev** | Placeholder for ECS deployment |
| **Smoke Tests** | Checks service health endpoints |

## Docker Image Naming

After successful build, images are available as:

```
docker.io/himanshu30243/user-service:1-abc123de
docker.io/himanshu30243/user-service:latest
docker.io/himanshu30243/family-service:1-abc123de
docker.io/himanshu30243/family-service:latest
... (same for all 6 services)
docker.io/himanshu30243/agrawalpulse-frontend:1-abc123de
docker.io/himanshu30243/agrawalpulse-frontend:latest
```

Where:
- `1` = Build number
- `abc123de` = Short git commit hash

## Troubleshooting

### Build fails at "Build Backend"
- Check Java version: `java -version` should show Java 21
- Check Maven: `mvn --version`
- Look at console for actual error

### Build fails at "Build Frontend"
- Check Node.js: `node --version` should show v20+
- Check npm: `npm --version`
- Look for missing dependencies in npm install

### Docker images not building
- Check Docker is running: `docker --version`
- Check Docker daemon is accessible: `docker info`
- Jenkins user may need Docker permission: `usermod -aG docker jenkins`

### Push to registry fails
- Verify credentials in Jenkins
- Check Docker Hub/registry credentials are correct
- Verify internet connectivity from Jenkins

## Customization

### Build Only on Specific Branches
Edit the `when` conditions in Jenkinsfile:
```groovy
when {
  branch 'main'  // Only build on main branch
}
```

Change to:
```groovy
when {
  branch 'develop'  // Or 'develop', 'staging', etc
}
```

### Add Slack Notifications
In Jenkinsfile `post` section, uncomment and customize:
```groovy
slackSend(
  color: 'good',
  message: "Build Succeeded: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
  channel: '#deployments'
)
```

### Add Email Notifications
In Jenkinsfile `post` section, add:
```groovy
emailext(
  subject: "Build ${currentBuild.result}: ${env.JOB_NAME}",
  body: "Build log: ${env.BUILD_URL}",
  to: "${env.CHANGE_AUTHOR_EMAIL}"
)
```

### Parallel Stages
For faster builds, run backend and frontend in parallel:
```groovy
stage('Build') {
  parallel {
    stage('Build Backend') { ... }
    stage('Build Frontend') { ... }
  }
}
```

## Monitoring & Logs

### View Build Logs
1. Go to job
2. Click build number (e.g., `#5`)
3. Click **Console Output**

### Check Service Health
After deployment, test endpoints:
```bash
curl http://localhost:8081/actuator/health  # user-service
curl http://localhost:8082/actuator/health  # family-service
```

## Next Steps

1. **Connect to ECS**: Update the "Deploy to Dev" stage to push to AWS ECS
2. **Add integration tests**: Add contract tests for cross-service REST calls
3. **Performance testing**: Add load testing stage
4. **Security scanning**: Add OWASP dependency check
5. **Artifact storage**: Configure Jenkins to archive build artifacts

## References

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Docker Plugin](https://plugins.jenkins.io/docker-build-step/)
- [Maven Plugin](https://plugins.jenkins.io/maven-plugin/)
- [GitHub Webhook Setup](https://docs.github.com/en/developers/webhooks-and-events/webhooks/creating-webhooks)
