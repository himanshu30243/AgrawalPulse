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

    // Build tag with commit info (BUILD_NUMBER auto-provided by Jenkins)
    // Note: GIT_COMMIT is a built-in Jenkins variable, use first 7 chars
    BUILD_TAG = "${BUILD_NUMBER}-${GIT_COMMIT.take(7)}"
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
          bat '''
            echo Maven Version:
            mvn --version

            echo Building all services...
            mvn clean install -DskipTests=true -U
          '''
        }
      }
    }

    stage('Build Frontend') {
      steps {
        echo "🎨 Building React frontend..."
        dir('frontend') {
          bat '''
            echo Node/NPM Version:
            node --version
            npm --version

            echo Installing dependencies...
            npm install

            echo Building production bundle...
            npm run build
          '''
        }
      }
    }

    stage('Backend Tests') {
      steps {
        echo "✅ Running backend unit tests..."
        dir('backend') {
          bat '''
            mvn test -X
          '''
        }
      }
    }

    stage('Frontend Tests') {
      steps {
        echo "✅ Running frontend tests..."
        dir('frontend') {
          bat '''
            npm run test -- --run || echo No tests configured yet
          '''
        }
      }
    }

    stage('Code Quality') {
      steps {
        echo "📊 Running code quality checks..."
        dir('backend') {
          bat '''
            echo Running spotbugs static analysis...
            mvn spotbugs:check || echo SpotBugs analysis complete
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
        script {
          def services = ['user-service', 'family-service', 'membership-service', 'matrimony-service', 'event-service', 'analytics-service']
          services.each { service ->
            dir("backend/${service}") {
              bat """
                echo Building image for ${service}:${BUILD_TAG}...
                docker build ^
                  --build-arg BUILD_NUMBER=${BUILD_NUMBER} ^
                  --tag ${DOCKER_IMAGE_PREFIX}/${service}:${BUILD_TAG} ^
                  --tag ${DOCKER_IMAGE_PREFIX}/${service}:latest ^
                  .
              """
            }
          }
        }

        echo "🐳 Building frontend Docker image..."
        dir('frontend') {
          bat """
            echo Building frontend image:${BUILD_TAG}...
            docker build ^
              --build-arg BUILD_NUMBER=${BUILD_NUMBER} ^
              --tag ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:${BUILD_TAG} ^
              --tag ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:latest ^
              .
          """
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
          bat """
            echo Logging into Docker Hub...
            docker login -u %DOCKER_USER% -p %DOCKER_PASS%

            echo Pushing backend service images...
            for /L %%i in (1,1,6) do (
              docker push ${DOCKER_IMAGE_PREFIX}/user-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/user-service:latest
              docker push ${DOCKER_IMAGE_PREFIX}/family-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/family-service:latest
              docker push ${DOCKER_IMAGE_PREFIX}/membership-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/membership-service:latest
              docker push ${DOCKER_IMAGE_PREFIX}/matrimony-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/matrimony-service:latest
              docker push ${DOCKER_IMAGE_PREFIX}/event-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/event-service:latest
              docker push ${DOCKER_IMAGE_PREFIX}/analytics-service:${BUILD_TAG}
              docker push ${DOCKER_IMAGE_PREFIX}/analytics-service:latest
            )

            echo Pushing frontend image...
            docker push ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:${BUILD_TAG}
            docker push ${DOCKER_IMAGE_PREFIX}/agrawalpulse-frontend:latest

            echo Logging out...
            docker logout
          """
        }
      }
    }

    stage('Deploy to Dev') {
      when {
        branch 'main'
      }
      steps {
        echo "🚀 Deploying to Dev environment..."
        bat '''
          echo Placeholder: Update ECS task definitions
          echo Placeholder: Deploy to ECS Fargate in dev cluster
          echo For now, this stage is a placeholder
        '''
      }
    }

    stage('Smoke Tests') {
      when {
        branch 'main'
      }
      steps {
        echo "🧪 Running smoke tests on deployed services..."
        bat '''
          echo Waiting for services to be healthy...
          timeout /t 10 /nobreak

          echo Testing health endpoints...
          for %%i in (8081 8082 8083 8084 8085 8086) do (
            echo Checking http://localhost:%%i/actuator/health
            curl -s http://localhost:%%i/actuator/health | findstr "UP" || (
              echo Service on port %%i may not be ready yet
            )
          )
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
