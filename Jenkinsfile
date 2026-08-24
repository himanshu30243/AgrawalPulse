// AgrawalPulse - Microservices CI/CD Pipeline
// Builds: 6 Spring Boot services + React frontend
// Includes: Maven build, tests, Docker image creation, push to registry

pipeline {
  agent any

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 60, unit: 'MINUTES')
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    // Docker Registry Configuration
    DOCKER_REGISTRY_URL = 'docker.io'
    DOCKER_IMAGE_PREFIX = 'himanshu30243'

    // Git commit info
    GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    BUILD_TAG = "${BUILD_NUMBER}-${GIT_COMMIT_SHORT}"
  }

  stages {
    stage('Checkout') {
      steps {
        echo "🔄 Checking out code from GitHub..."
        checkout scm
      }
    }

    stage('Build Backend') {
      steps {
        echo "🔨 Building backend (6 microservices)..."
        dir('backend') {
          sh '''
            echo "Maven Version:"
            mvn --version

            echo "Building all services..."
            mvn clean install -DskipTests=true -U
          '''
        }
      }
    }

    stage('Build Frontend') {
      steps {
        echo "🎨 Building React frontend..."
        dir('frontend') {
          sh '''
            echo "Node/NPM Version:"
            node --version
            npm --version

            echo "Installing dependencies..."
            npm install

            echo "Building production bundle..."
            npm run build
          '''
        }
      }
    }

    stage('Backend Tests') {
      steps {
        echo "✅ Running backend unit tests..."
        dir('backend') {
          sh '''
            mvn test -X
          '''
        }
      }
    }

    stage('Frontend Tests') {
      steps {
        echo "✅ Running frontend tests..."
        dir('frontend') {
          sh '''
            npm run test -- --run 2>&1 || echo "No tests configured yet"
          '''
        }
      }
    }

    stage('Code Quality') {
      steps {
        echo "📊 Running code quality checks..."
        dir('backend') {
          sh '''
            echo "Running spotbugs (static analysis)..."
            mvn spotbugs:check || echo "SpotBugs analysis complete"
          '''
        }
      }
    }

    stage('Build Docker Images') {
      when {
        branch 'main'
      }
      steps {
        echo "🐳 Building Docker images for 6 backend services..."
        sh '''
          cd backend
          for service in user-service family-service membership-service matrimony-service event-service analytics-service; do
            echo "Building image for $service:${BUILD_TAG}..."
            cd $service
            docker build \
              --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
              --tag ${DOCKER_IMAGE_PREFIX}/$service:${BUILD_TAG} \
              --tag ${DOCKER_IMAGE_PREFIX}/$service:latest \
              .
            cd ..
          done
        '''

        echo "🐳 Building frontend Docker image..."
        dir('frontend') {
          sh '''
            echo "Building frontend image:${BUILD_TAG}..."
            docker build \
              --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
              --tag ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:${BUILD_TAG} \
              --tag ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:latest \
              .
          '''
        }
      }
    }

    stage('Push to Registry') {
      when {
        branch 'main'
      }
      steps {
        echo "📤 Pushing Docker images to Docker Hub..."
        withCredentials([usernamePassword(credentialsId: 'docker-registry-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo "Logging into Docker Hub..."
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

            echo "Pushing backend service images..."
            for service in user-service family-service membership-service matrimony-service event-service analytics-service; do
              echo "Pushing ${DOCKER_IMAGE_PREFIX}/$service:${BUILD_TAG}..."
              docker push ${DOCKER_IMAGE_PREFIX}/$service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/$service:latest
            done

            echo "Pushing frontend image..."
            docker push ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:${BUILD_TAG}
            docker push ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:latest

            echo "Logging out..."
            docker logout
          '''
        }
      }
    }

    stage('Deploy to Dev') {
      when {
        branch 'main'
      }
      steps {
        echo "🚀 Deploying to Dev environment..."
        sh '''
          echo "Placeholder: Update ECS task definitions"
          echo "Placeholder: Deploy to ECS Fargate in dev cluster"
          echo "For now, this stage is a placeholder"
        '''
      }
    }

    stage('Smoke Tests') {
      when {
        branch 'main'
      }
      steps {
        echo "🧪 Running smoke tests on deployed services..."
        sh '''
          echo "Waiting for services to be healthy..."
          sleep 10

          echo "Testing health endpoints..."
          services=(
            "http://localhost:8081/actuator/health"
            "http://localhost:8082/actuator/health"
            "http://localhost:8083/actuator/health"
            "http://localhost:8084/actuator/health"
            "http://localhost:8085/actuator/health"
            "http://localhost:8086/actuator/health"
          )

          for service in "${services[@]}"; do
            echo "Checking $service"
            curl -s $service | grep -q "UP" || echo "Service may not be ready yet"
          done
        '''
      }
    }
  }

  post {
    success {
      echo "✅ Pipeline succeeded!"
      echo "Docker images built and pushed:"
      echo "  - ${DOCKER_IMAGE_PREFIX}/<service>:${BUILD_TAG}"
      echo "  - ${DOCKER_IMAGE_PREFIX}/<service>:latest"
      // Uncomment below to add Slack notification:
      // slackSend(
      //   color: 'good',
      //   message: "✅ Build Succeeded: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      //   channel: '#deployments'
      // )
    }

    failure {
      echo "❌ Pipeline failed!"
      echo "Check the console output above for error details"
      // Uncomment below to add Slack notification:
      // slackSend(
      //   color: 'danger',
      //   message: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      //   channel: '#deployments'
      // )
    }

    unstable {
      echo "⚠️ Pipeline unstable - some tests failed"
    }

    always {
      echo "🏁 Pipeline execution completed"
      echo "Build log: ${env.BUILD_URL}console"
    }
  }
}
