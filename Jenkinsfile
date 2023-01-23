@Library('pipeline-library-helm-chart@3.0.2')
@Library('pipeline-general@5.0.11') _

String slackChannel = '#em-pipeline'
Map<String, ?> config = [:]
Map<String, ?> fossaScanOptions = config.fossaScanOptions ?: [:] as Map<String, ?>

pipeline {
    agent any
    tools {
        jdk 'collibra-zulu11.60.19-jdk11.0.17'
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '40'))
        timestamps()
        durabilityHint('PERFORMANCE_OPTIMIZED')
    }
    environment {
        NEXUS = credentials('nexus-gradle')
        GRADLE_ENTERPRISE_ACCESS_KEY = credentials('GRADLE_ENTERPRISE_ACCESS_KEY')
		VERSION = '0.0.1-jdk11'
    }
    parameters {
        booleanParam(name: 'SKIP_PUBLISH', defaultValue: false, description: 'skip publish step')
        booleanParam(name: 'FORCE_DOCS_PUBLISH', defaultValue: false, description: 'force a structurizr & devdocs publish')
    }
    stages {
        stage('Setup') {
            steps {
                ontrackSetup(
                    autoValidationStampCreation: true,
                    autoPromotions: [
                        IRON: [validations: ['build']],
                        GOLD: [
                            validations: ['sonarqube', 'tagging', 'nexus'],
                            promotions : ['IRON'],
                        ]
                    ]
                )
            }
        }
        stage('Build') {
            steps {
                script {
                    doGradle('build')
                }
            }
            post {
                always {
                    script {
                        def results = ontrackValidationForJUnit pattern: '**/build/test-results/**/*.xml', build: env.VERSION, validation: 'build'
                        slackStageNotification channel: slackChannel, results: results
                    }
                }
            }
        }
        stage('Fossa Scan') {
            when {
                branch 'main'
            }
            steps {
                script {
                    fossaScan(fossaScanOptions)
                }
            }
            post {
                always {
                    ontrackSetValidation build: env.VERSION, validation: 'fossaScan'
                }
            }
        }
        stage('Publication') {
            when {
                allOf {
                    not {
                        expression { params.SKIP_PUBLISH == true }
                    }
					branch 'main_jdk11'
                }
            }
            steps {
                script {
                    currentBuild.description = "Version ${env.VERSION}"
                    doGradle('publish')
                }
                tagging(
                        name: env.VERSION,
                        commit: env.GIT_COMMIT,
                )
            }
            post {
                always {
                    slackStageNotification channel: slackChannel
                    ontrackSetValidation build: env.VERSION, validation: 'tagging'
                    ontrackSetValidation build: env.VERSION, validation: 'nexus'
                }
            }
        }
        stage('Dev Documentation') {
            when {
                beforeAgent true
                anyOf {
                    expression { params.FORCE_DOCS_PUBLISH == true }
					branch 'main_jdk11'
                }
            }
            environment {
                GRGIT_USER = credentials('github-collibra-cicd-token')
            }
            steps {
                withCredentials([
                    usernamePassword(credentialsId: 'nexus-gradle', usernameVariable: 'NEXUS_USR', passwordVariable: 'NEXUS_PSW'),
                ]) {
					doGradle('gitPublishPush')
                }
            }
        }
    }
}

def doGradle(String... tasks) {
    sh """\
        ./gradlew \\
        ${tasks.join(' ')} \\
        -PnexusUserName=${NEXUS_USR} \\
        -PnexusPassword=${NEXUS_PSW} \\
        --console plain \\
        --info \\
        --stacktrace
    """
}
