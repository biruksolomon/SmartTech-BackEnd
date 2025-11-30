# SmartTech E-Commerce & Maintenance Management System

A comprehensive Spring Boot application for **Sentayehu Abebe Computer Retail Trade and Maintenance** - a full-featured e-commerce platform with integrated device maintenance services, built for the Ethiopian market.

## 🚀 Project Overview

SmartTech is an enterprise-grade retail management system that combines e-commerce functionality with professional maintenance services. The platform serves both B2C customers purchasing technology products and B2B maintenance service requests, featuring integrated payment processing through Chapa (Ethiopian payment gateway), multi-tier customer loyalty programs, and comprehensive business analytics.

## 🏗️ Architecture & Technology Stack

### **Backend Framework**
- **Spring Boot 3.2.0** with Java 17
- **Spring Security** with JWT authentication
- **Spring Data JPA** with Hibernate
- **MySQL 8.0** database
- **Redis** for caching and session management

### **Payment Integration**
- **Chapa Payment Gateway** for Ethiopian market
- Webhook-based payment verification
- Multi-payment method support

### **File Storage**
- **Unified Storage System** (Local + AWS S3)
- Automatic failover capabilities
- Support for invoices, maintenance documents, and product images

### **Communication Services**
- **Email Service** with Gmail SMTP
- **SMS Integration** via AfroMessage API
- **OTP Verification** system

### **Documentation & Testing**
- **OpenAPI/Swagger** documentation
- **JUnit 5** with Mockito
- **TestContainers** for integration testing
- **JaCoCo** code coverage (80% minimum)

## 📊 Core Business Features

### **E-Commerce Platform**
- **Product Catalog Management** with categories and search
- **Inventory Management** with low-stock alerts
- **Order Processing** with complete lifecycle tracking
- **Customer Tier System** (Bronze, Silver, Gold, Diamond)
- **Shopping Cart** and checkout functionality

### **Maintenance Services**
- **Service Request Management** for device repairs
- **Maintenance Ticket Generation** with PDF reports
- **Warranty Tracking** and coverage verification
- **Service Status Updates** with customer notifications

### **Payment Processing**
- **Chapa Integration** for Ethiopian payments
- **Payment Verification** via webhooks
- **Invoice Generation** with PDF export
- **VAT Calculation** (15% Ethiopian tax rate)

### **User Management**
- **Role-Based Access Control** (Customer, Admin, Super Admin)
- **Email & Phone Verification** with OTP
- **Customer Loyalty Tiers** based on purchase history
- **Account Management** with profile updates

### **Analytics & Reporting**
- **Dashboard Statistics** with real-time metrics
- **Sales Reports** with date range filtering
- **Monthly Tax Reports** for ERCA compliance
- **Low Stock Monitoring** with automated alerts

## 🗂️ Project Structure


src/main/java/com/smarttech/
├── entity/                 
│   ├── User.java          
│   ├── Product.java       
│   ├── Order.java         
│   ├── Payment.java       
│   └── MaintenanceRequest.java 
├── controller/            
│   ├── AuthController.java      
│   ├── ProductController.java   
│   ├── OrderController.java     
│   └── MaintenanceController.java 
├── service/               
│   ├── impl/             
│   ├── AuthService.java  
│   ├── PaymentService.java 
│   └── AnalyticsService.java 
├── config/               
│   ├── SecurityConfig.java     
│   ├── FileStorageConfig.java  
│   └── AwsConfig.java          
└── enums/                
    ├── UserRole.java     
    ├── OrderStatus.java  
    └── CustomerTier.java 

## 🔧 Key Technical Features

### **Security Implementation**
- **JWT-based Authentication** with role-based authorization
- **Password Encryption** using BCrypt
- **CORS Configuration** for cross-origin requests
- **Method-level Security** with @PreAuthorize annotations

### **File Management**
- **Multi-Provider Storage** (Local filesystem + AWS S3)
- **Automatic Failover** between storage providers
- **File Type Validation** and size limits
- **PDF Generation** for invoices and maintenance tickets

### **Communication System**
- **Multi-Channel Notifications** (Email + SMS)
- **Template-Based Messaging** for consistent communication
- **OTP Verification** with rate limiting and expiry
- **Webhook Processing** for payment confirmations

### **Data Management**
- **Entity Relationships** with proper foreign key constraints
- **Audit Trails** with creation/update timestamps
- **Soft Deletes** for data integrity
- **Database Migrations** with Liquibase

## 🚀 API Endpoints

### **Authentication**
- `POST /auth/login` - User authentication
- `POST /auth/register` - Customer registration
- `POST /auth/send-phone-otp` - OTP verification
- `POST /auth/verify-email-code` - Email verification

### **Product Management**
- `GET /products` - Browse product catalog
- `GET /products/search` - Search products
- `POST /products` - Create product (Admin)
- `PUT /products/{id}/stock` - Update inventory (Admin)

### **Order Processing**
- `POST /orders` - Create new order
- `GET /orders/my` - Customer order history
- `PUT /orders/{id}/status` - Update order status (Admin)

### **Maintenance Services**
- `POST /maintenance/requests` - Submit service request
- `GET /maintenance/requests/my` - Customer service history
- `POST /maintenance/requests/{id}/approve` - Approve service (Admin)

## 📈 Business Logic Highlights

### **Customer Tier System**
Automatic tier calculation based on total purchases:
- **Bronze**: ETB 0 - 50,000
- **Silver**: ETB 50,000 - 150,000  
- **Gold**: ETB 150,000 - 500,000
- **Diamond**: ETB 500,000+

### **Order Lifecycle Management**
Complete order status tracking:
`PENDING → PAYMENT_PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`

### **Maintenance Request Flow**
Service request processing:
`PENDING → APPROVED → IN_PROGRESS → COMPLETED → DELIVERED`

### **Payment Integration**
- Chapa payment gateway integration
- Webhook signature verification
- Automatic order status updates
- Invoice generation with VAT calculation

## 🔒 Security Features

- **JWT Token Authentication** with configurable expiration
- **Role-Based Access Control** with method-level security
- **Input Validation** with Bean Validation annotations
- **SQL Injection Prevention** through JPA/Hibernate
- **CORS Protection** with configurable origins
- **Rate Limiting** for OTP requests

## 📊 Monitoring & Analytics

- **Real-time Dashboard** with key business metrics
- **Sales Performance Tracking** with revenue analytics
- **Inventory Monitoring** with automated low-stock alerts
- **Customer Analytics** with tier distribution
- **Payment Success Rates** and transaction monitoring

## 🛠️ Development Features

### **Testing Strategy**
- **Unit Tests** with JUnit 5 and Mockito
- **Integration Tests** with TestContainers
- **Code Coverage** monitoring with JaCoCo
- **API Documentation** with OpenAPI/Swagger

### **Configuration Management**
- **Environment-based Configuration** with Spring Profiles
- **External Configuration** via application.yml
- **Feature Toggles** for storage providers and SMS services
- **Health Checks** for external service dependencies

## 🚀 Deployment & Scalability

### **Production Ready Features**
- **Docker Support** with multi-stage builds
- **Health Endpoints** for monitoring
- **Graceful Shutdown** handling
- **Connection Pooling** for database optimization
- **Caching Strategy** with Redis integration

### **Scalability Considerations**
- **Stateless Architecture** for horizontal scaling
- **Database Connection Pooling** for performance
- **File Storage Abstraction** for cloud migration
- **Event-Driven Architecture** for loose coupling

## 📋 Business Requirements Addressed

1. **E-Commerce Functionality** - Complete online store with product catalog, shopping cart, and checkout
2. **Maintenance Services** - Professional device repair and service request management
3. **Payment Processing** - Local payment gateway integration for Ethiopian market
4. **Customer Management** - Tiered loyalty system with personalized experiences
5. **Inventory Management** - Real-time stock tracking with automated alerts
6. **Financial Reporting** - Tax-compliant reporting for Ethiopian business requirements
7. **Multi-Channel Communication** - Email and SMS notifications for customer engagement

## 🎯 Key Achievements

- **Full-Stack Enterprise Application** with modern Spring Boot architecture
- **Payment Gateway Integration** specifically for Ethiopian market (Chapa)
- **Multi-Tier Customer System** with automated tier calculation
- **Comprehensive Security Implementation** with JWT and role-based access
- **Unified File Storage System** with automatic failover capabilities
- **Business Analytics Dashboard** with real-time metrics
- **Tax-Compliant Reporting** for Ethiopian business requirements
- **Professional Documentation** with OpenAPI/Swagger integration

---

**Built for:** Portfolio demonstration of enterprise-level Spring Boot development  
**Target Market:** Ethiopian technology retail and maintenance services  
**Architecture:** Microservices-ready with clean separation of concerns  
**Scalability:** Designed for horizontal scaling and cloud deployment

