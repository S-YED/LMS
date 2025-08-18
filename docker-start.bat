@echo off
REM Docker Startup Script for Leave Management System (Windows)
REM This script provides easy commands to manage the Docker environment

setlocal enabledelayedexpansion

REM Function to print status messages
:print_status
echo [INFO] %~1
goto :eof

:print_warning
echo [WARNING] %~1
goto :eof

:print_error
echo [ERROR] %~1
goto :eof

:print_header
echo ================================
echo %~1
echo ================================
goto :eof

REM Function to check if Docker is running
:check_docker
docker info >nul 2>&1
if errorlevel 1 (
    call :print_error "Docker is not running. Please start Docker and try again."
    exit /b 1
)
goto :eof

REM Function to check if .env file exists
:check_env_file
if not exist .env (
    call :print_warning ".env file not found. Creating from .env.example..."
    if exist .env.example (
        copy .env.example .env >nul
        call :print_warning "Please update the .env file with your configuration before running production."
    ) else (
        call :print_error ".env.example file not found. Please create .env file manually."
        exit /b 1
    )
)
goto :eof

REM Function to build the application
:build_app
call :print_header "Building Application"
call :print_status "Building Spring Boot application..."
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    call :print_error "Build failed!"
    exit /b 1
)
call :print_status "Application built successfully!"
goto :eof

REM Function to start development environment
:start_dev
call :print_header "Starting Development Environment"
call :check_docker
if errorlevel 1 exit /b 1

call :print_status "Starting services with docker-compose..."
docker-compose up -d
if errorlevel 1 (
    call :print_error "Failed to start development environment!"
    exit /b 1
)

call :print_status "Waiting for services to be ready..."
timeout /t 30 /nobreak >nul

call :print_status "Development environment started successfully!"
call :print_status "Application: http://localhost:8080"
call :print_status "Swagger UI: http://localhost:8080/swagger-ui.html"
call :print_status "MailHog UI: http://localhost:8025"
call :print_status "Adminer: http://localhost:8081"
call :print_status "Health Check: http://localhost:8080/actuator/health"
goto :eof

REM Function to start production environment
:start_prod
call :print_header "Starting Production Environment"
call :check_docker
if errorlevel 1 exit /b 1
call :check_env_file
if errorlevel 1 exit /b 1

call :print_status "Starting production services..."
docker-compose -f docker-compose.prod.yml up -d
if errorlevel 1 (
    call :print_error "Failed to start production environment!"
    exit /b 1
)

call :print_status "Waiting for services to be ready..."
timeout /t 60 /nobreak >nul

call :print_status "Production environment started successfully!"
call :print_status "Application: http://localhost"
call :print_status "Grafana: http://localhost:3000"
call :print_status "Prometheus: http://localhost:9090"
call :print_status "Health Check: http://localhost/actuator/health"
goto :eof

REM Function to stop services
:stop_services
call :print_header "Stopping Services"

if "%~1"=="prod" (
    call :print_status "Stopping production services..."
    docker-compose -f docker-compose.prod.yml down
) else (
    call :print_status "Stopping development services..."
    docker-compose down
)

call :print_status "Services stopped successfully!"
goto :eof

REM Function to view logs
:view_logs
call :print_header "Viewing Logs"

if "%~1"=="prod" (
    if not "%~2"=="" (
        docker-compose -f docker-compose.prod.yml logs -f %~2
    ) else (
        docker-compose -f docker-compose.prod.yml logs -f
    )
) else (
    if not "%~2"=="" (
        docker-compose logs -f %~2
    ) else (
        docker-compose logs -f
    )
)
goto :eof

REM Function to show status
:show_status
call :print_header "Service Status"

if "%~1"=="prod" (
    docker-compose -f docker-compose.prod.yml ps
) else (
    docker-compose ps
)
goto :eof

REM Function to clean up
:cleanup
call :print_header "Cleaning Up"

call :print_status "Stopping and removing containers..."
docker-compose down -v
docker-compose -f docker-compose.prod.yml down -v 2>nul

call :print_status "Removing unused images..."
docker image prune -f

call :print_status "Cleanup completed!"
goto :eof

REM Function to run database migration
:run_migration
call :print_header "Running Database Migration"

call :print_status "Executing database migration..."
docker-compose exec mysql mysql -u leave_user -pleave_password leave_management_db < src/main/resources/db/migration/V1__create_schema.sql
docker-compose exec mysql mysql -u leave_user -pleave_password leave_management_db < src/main/resources/db/migration/V2__insert_sample_data.sql

call :print_status "Database migration completed!"
goto :eof

REM Function to backup database
:backup_database
call :print_header "Backing Up Database"

for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "BACKUP_FILE=backup_%YYYY%%MM%%DD%_%HH%%Min%%Sec%.sql"

call :print_status "Creating database backup: %BACKUP_FILE%"
docker-compose exec mysql mysqldump -u leave_user -pleave_password leave_management_db > %BACKUP_FILE%

call :print_status "Database backup created: %BACKUP_FILE%"
goto :eof

REM Function to show help
:show_help
echo Leave Management System - Docker Management Script (Windows)
echo.
echo Usage: %~nx0 [COMMAND] [OPTIONS]
echo.
echo Commands:
echo   build                 Build the Spring Boot application
echo   dev                   Start development environment
echo   prod                  Start production environment
echo   stop [dev^|prod]       Stop services (default: dev)
echo   logs [dev^|prod] [service]  View logs
echo   status [dev^|prod]     Show service status
echo   cleanup               Clean up containers and images
echo   migrate               Run database migration
echo   backup                Backup database
echo   help                  Show this help message
echo.
echo Examples:
echo   %~nx0 build              # Build the application
echo   %~nx0 dev                # Start development environment
echo   %~nx0 prod               # Start production environment
echo   %~nx0 logs dev app       # View logs for app service in dev
echo   %~nx0 stop prod          # Stop production environment
echo   %~nx0 cleanup            # Clean up everything
goto :eof

REM Main script logic
if "%~1"=="build" (
    call :build_app
) else if "%~1"=="dev" (
    call :start_dev
) else if "%~1"=="prod" (
    call :start_prod
) else if "%~1"=="stop" (
    call :stop_services %~2
) else if "%~1"=="logs" (
    call :view_logs %~2 %~3
) else if "%~1"=="status" (
    call :show_status %~2
) else if "%~1"=="cleanup" (
    call :cleanup
) else if "%~1"=="migrate" (
    call :run_migration
) else if "%~1"=="backup" (
    call :backup_database
) else if "%~1"=="help" (
    call :show_help
) else if "%~1"=="--help" (
    call :show_help
) else if "%~1"=="-h" (
    call :show_help
) else if "%~1"=="" (
    call :show_help
) else (
    call :print_error "Unknown command: %~1"
    echo.
    call :show_help
    exit /b 1
)

endlocal