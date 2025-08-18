#!/bin/bash

# Docker Startup Script for Leave Management System
# This script provides easy commands to manage the Docker environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to check if .env file exists
check_env_file() {
    if [ ! -f .env ]; then
        print_warning ".env file not found. Creating from .env.example..."
        if [ -f .env.example ]; then
            cp .env.example .env
            print_warning "Please update the .env file with your configuration before running production."
        else
            print_error ".env.example file not found. Please create .env file manually."
            exit 1
        fi
    fi
}

# Function to build the application
build_app() {
    print_header "Building Application"
    print_status "Building Spring Boot application..."
    ./mvnw.cmd clean package -DskipTests
    print_status "Application built successfully!"
}

# Function to start development environment
start_dev() {
    print_header "Starting Development Environment"
    check_docker
    
    print_status "Starting services with docker-compose..."
    docker-compose up -d
    
    print_status "Waiting for services to be ready..."
    sleep 30
    
    print_status "Development environment started successfully!"
    print_status "Application: http://localhost:8080"
    print_status "Swagger UI: http://localhost:8080/swagger-ui.html"
    print_status "MailHog UI: http://localhost:8025"
    print_status "Adminer: http://localhost:8081"
    print_status "Health Check: http://localhost:8080/actuator/health"
}

# Function to start production environment
start_prod() {
    print_header "Starting Production Environment"
    check_docker
    check_env_file
    
    print_status "Starting production services..."
    docker-compose -f docker-compose.prod.yml up -d
    
    print_status "Waiting for services to be ready..."
    sleep 60
    
    print_status "Production environment started successfully!"
    print_status "Application: http://localhost"
    print_status "Grafana: http://localhost:3000"
    print_status "Prometheus: http://localhost:9090"
    print_status "Health Check: http://localhost/actuator/health"
}

# Function to stop services
stop_services() {
    print_header "Stopping Services"
    
    if [ "$1" = "prod" ]; then
        print_status "Stopping production services..."
        docker-compose -f docker-compose.prod.yml down
    else
        print_status "Stopping development services..."
        docker-compose down
    fi
    
    print_status "Services stopped successfully!"
}

# Function to view logs
view_logs() {
    print_header "Viewing Logs"
    
    if [ "$1" = "prod" ]; then
        if [ -n "$2" ]; then
            docker-compose -f docker-compose.prod.yml logs -f "$2"
        else
            docker-compose -f docker-compose.prod.yml logs -f
        fi
    else
        if [ -n "$2" ]; then
            docker-compose logs -f "$2"
        else
            docker-compose logs -f
        fi
    fi
}

# Function to show status
show_status() {
    print_header "Service Status"
    
    if [ "$1" = "prod" ]; then
        docker-compose -f docker-compose.prod.yml ps
    else
        docker-compose ps
    fi
}

# Function to clean up
cleanup() {
    print_header "Cleaning Up"
    
    print_status "Stopping and removing containers..."
    docker-compose down -v
    docker-compose -f docker-compose.prod.yml down -v 2>/dev/null || true
    
    print_status "Removing unused images..."
    docker image prune -f
    
    print_status "Cleanup completed!"
}

# Function to run database migration
run_migration() {
    print_header "Running Database Migration"
    
    print_status "Executing database migration..."
    docker-compose exec mysql mysql -u leave_user -pleave_password leave_management_db < src/main/resources/db/migration/V1__create_schema.sql
    docker-compose exec mysql mysql -u leave_user -pleave_password leave_management_db < src/main/resources/db/migration/V2__insert_sample_data.sql
    
    print_status "Database migration completed!"
}

# Function to backup database
backup_database() {
    print_header "Backing Up Database"
    
    BACKUP_FILE="backup_$(date +%Y%m%d_%H%M%S).sql"
    
    print_status "Creating database backup: $BACKUP_FILE"
    docker-compose exec mysql mysqldump -u leave_user -pleave_password leave_management_db > "$BACKUP_FILE"
    
    print_status "Database backup created: $BACKUP_FILE"
}

# Function to show help
show_help() {
    echo "Leave Management System - Docker Management Script"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  build                 Build the Spring Boot application"
    echo "  dev                   Start development environment"
    echo "  prod                  Start production environment"
    echo "  stop [dev|prod]       Stop services (default: dev)"
    echo "  logs [dev|prod] [service]  View logs"
    echo "  status [dev|prod]     Show service status"
    echo "  cleanup               Clean up containers and images"
    echo "  migrate               Run database migration"
    echo "  backup                Backup database"
    echo "  help                  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 build              # Build the application"
    echo "  $0 dev                # Start development environment"
    echo "  $0 prod               # Start production environment"
    echo "  $0 logs dev app       # View logs for app service in dev"
    echo "  $0 stop prod          # Stop production environment"
    echo "  $0 cleanup            # Clean up everything"
}

# Main script logic
case "$1" in
    build)
        build_app
        ;;
    dev)
        start_dev
        ;;
    prod)
        start_prod
        ;;
    stop)
        stop_services "$2"
        ;;
    logs)
        view_logs "$2" "$3"
        ;;
    status)
        show_status "$2"
        ;;
    cleanup)
        cleanup
        ;;
    migrate)
        run_migration
        ;;
    backup)
        backup_database
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac