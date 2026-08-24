# RBAC System Backend Implementation Guide

## Overview
Comprehensive Role-Based Access Control (RBAC) system with support for roles, permissions, menus, branches, and audit logging.

---

## Database Schema

### Tables Created (Migration V2)

#### 1. **roles**
- System roles and custom roles
- Fields: id, name, description, is_system, is_active, created_at, updated_at
- Default roles: Admin, User, Chapter Admin, National Admin

#### 2. **permissions**
- Granular permissions by category
- Fields: id, code, name, description, category, is_active, created_at
- 19 default permissions across categories (Users, Roles, Branches, Reports, etc.)

#### 3. **branches**
- Organization branches/chapters
- Fields: id, name, code, state, city, location, address, contact_number, email, parent_branch_id, is_active, created_at, updated_at
- Supports hierarchical branches

#### 4. **menus**
- Navigation menu items
- Fields: id, name, slug, icon, route_path, display_order, parent_id, description, is_active, created_at, updated_at
- 7 default menus (Dashboard, Members, Families, Membership, Matrimony, Events, Administration)

#### 5. **role_permissions**
- Maps roles to permissions (many-to-many)
- Enables flexible permission assignment

#### 6. **menu_permissions**
- Maps menus to required permissions (many-to-many)
- Controls menu visibility based on permissions

#### 7. **role_branches**
- Maps roles to branches (optional role-specific branch restrictions)
- Supports branch-level role scoping

#### 8. **user_roles**
- Maps users to roles (many-to-many, supports multiple roles per user)
- Tracks who assigned the role and when

#### 9. **user_branches**
- Maps users to branches
- Supports multiple branch assignments per user
- is_primary field marks primary branch

#### 10. **audit_logs**
- Tracks all RBAC changes (CREATE, UPDATE, DELETE, ASSIGN, REMOVE)
- Fields: id, user_id, action, entity_type, entity_id, changes (JSON), created_at
- Enables compliance and debugging

---

## JPA Entities Created

### 1. **Role.java**
```
- Represents system and custom roles
- Many-to-many relationships with: Permission, Branch
- Audit fields: createdAt, updatedAt
```

### 2. **Permission.java**
```
- Represents granular permissions
- Fields: code (unique), name, category, description, isActive
```

### 3. **Branch.java**
```
- Represents organization branches
- Self-referencing: parent_branch_id for hierarchy
- Audit fields: createdAt, updatedAt
```

### 4. **Menu.java**
```
- Represents navigation menus
- Self-referencing: parent_id for nested menus
- Many-to-many with Permission
- Fields: displayOrder for ordering
```

### 5. **AuditLog.java**
```
- Tracks all RBAC system changes
- JSON changes field for detailed audit trail
- Supports action types and entity types (enums)
```

---

## Services & Implementation

### RoleService (Interface & Implementation)

**Methods:**
```
- getAllRoles()                          // List all roles
- getAllActiveRoles()                    // List active roles only
- getRoleById(Long id)                   // Get specific role with permissions
- getRoleByName(String name)             // Find role by name
- createRole(CreateRoleRequest)          // Create new role
- updateRole(Long id, CreateRoleRequest) // Update role details
- deleteRole(Long id)                    // Delete role (soft delete)
- assignPermissionsToRole(Long roleId, Set<Long> permissionIds) // Assign permissions
- removePermissionFromRole(Long roleId, Long permissionId)      // Remove permission
- getRolesByPermissionCode(String code)  // Find roles with specific permission
- assignBranchesToRole(Long roleId, Set<Long> branchIds)        // Assign branches
- canDeleteRole(Long roleId)             // Check if role can be deleted
- validateRoleExists(Long roleId)        // Validate role exists
- validateRoleNotSystem(Long roleId)     // Prevent system role modification
```

**Features:**
- Transaction management (@Transactional)
- Audit logging for all operations
- Validation and error handling
- Prevents modification of system roles
- Automatic createdAt/updatedAt timestamps

### BranchService (Interface & Implementation)

**Methods:**
```
- getAllBranches()                       // List all branches
- getAllActiveBranches()                 // List active branches
- getBranchById(Long id)                 // Get specific branch
- createBranch(CreateBranchRequest)      // Create new branch
- updateBranch(Long id, CreateBranchRequest)  // Update branch
- deleteBranch(Long id)                  // Delete branch (soft delete)
- getBranchesByState(String state)       // Find branches in state
- getRootBranches()                      // Get top-level branches
- validateBranchExists(Long branchId)    // Validate branch exists
```

**Features:**
- Hierarchical branch support (parent-child relationships)
- Soft delete (sets is_active = false)
- Audit logging
- Validation and error handling

### AuditLogService (Interface & Implementation)

**Methods:**
```
- logAction(String action, String entityType, Long entityId, Object oldValue, Object newValue)
- getAuditLogs(Pageable pageable)
- getAuditLogsByUserId(Long userId, Pageable pageable)
- getAuditLogsByEntityType(String entityType, Pageable pageable)
- getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable)
```

**Features:**
- JSON serialization of changes
- Automatic timestamping
- Pagination support
- Query logs by various criteria

---

## REST Controllers

### RoleController (`/api/v1/roles`)

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/` | roles.view | List all roles |
| GET | `/active` | roles.view | List active roles |
| GET | `/{id}` | roles.view | Get role details |
| POST | `/` | roles.create | Create new role |
| PUT | `/{id}` | roles.edit | Update role |
| DELETE | `/{id}` | roles.delete | Delete role |
| POST | `/{id}/permissions` | roles.edit | Assign permissions |
| DELETE | `/{id}/permissions/{permissionId}` | roles.edit | Remove permission |
| POST | `/{id}/branches` | roles.edit | Assign branches |
| GET | `/permission/{code}` | roles.view | Find roles by permission |

### BranchController (`/api/v1/branches`)

| Method | Endpoint | Permission | Description |
|--------|----------|-----------|-------------|
| GET | `/` | - | List all branches |
| GET | `/active` | - | List active branches |
| GET | `/{id}` | - | Get branch details |
| POST | `/` | branches.create | Create new branch |
| PUT | `/{id}` | branches.edit | Update branch |
| DELETE | `/{id}` | branches.delete | Delete branch |
| GET | `/state/{state}` | - | Find by state |
| GET | `/root` | - | Get root branches |

---

## DTOs

### CreateRoleRequest
```java
@NotBlank String name              // 2-100 chars
@Size(max=500) String description
```

### RoleDto
```java
Long id
String name
String description
Boolean isSystem
Boolean isActive
Set<Long> permissionIds
Integer permissionCount
Set<Long> branchIds
LocalDateTime createdAt
LocalDateTime updatedAt
```

### CreateBranchRequest
```java
@NotBlank String name              // 2-100 chars
@NotBlank String code              // 2-50 chars
@NotBlank String state
@NotBlank String city
String location
String address
String contactNumber
String email
Long parentBranchId                // Optional for hierarchical branches
```

### BranchDto
```java
Long id
String name
String code
String state
String city
String location
String address
String contactNumber
String email
Long parentBranchId
Boolean isActive
Integer userCount
LocalDateTime createdAt
LocalDateTime updatedAt
```

---

## Security Configuration

### Permission-Based Authorization

All endpoints use `@PreAuthorize` annotation:

```java
@PreAuthorize("hasAnyAuthority('roles.view')")
public List<RoleDto> getAllRoles() { ... }
```

### Required Security Setup

1. **Enable Method Security in SecurityConfig:**
```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig { ... }
```

2. **Configure Permission Provider:**
```java
@Bean
public UserDetailsService userDetailsService() {
  // Load user roles and permissions
  // Map permissions from user's roles
}
```

3. **Custom Authentication Provider:**
```java
@Component
public class RolePermissionProvider {
  public Collection<? extends GrantedAuthority> 
    getPermissionsForUser(Long userId) {
    // Fetch user's roles
    // Fetch permissions from each role
    // Return as GrantedAuthority list
  }
}
```

---

## Repositories

### RoleRepository
```
- findByName(String name)
- findAllActive()
- findAllCustomRoles()
- findAllByPermissionCode(String code)
- existsByName(String name)
```

### PermissionRepository
```
- findByCode(String code)
- findAllCategories()
- findByCategory(String category)
- findAllActive()
- findAllByIds(Set<Long> ids)
```

### BranchRepository
```
- findByCode(String code)
- findAllActive()
- findByState(String state)
- findAllRootBranches()
- existsByCode(String code)
```

### MenuRepository
```
- findBySlug(String slug)
- findAllRootMenus()
- findAllActive()
- findChildrenByParent(Long parentId)
- existsBySlug(String slug)
```

### AuditLogRepository
```
- findByUserId(Long userId, Pageable)
- findByEntityType(String entityType, Pageable)
- findByEntityTypeAndEntityId(String entityType, Long entityId)
- findByDateRange(LocalDateTime start, LocalDateTime end, Pageable)
```

---

## Default Data

### System Roles (4)
1. **Admin** - Full system access (is_system=true)
2. **User** - Self-service only (is_system=true)
3. **Chapter Admin** - Manage assigned chapter
4. **National Admin** - National-level access

### Default Permissions (19)
**Categories:**
- Users: view, create, edit, delete
- Roles: view, create, edit, delete
- Branches: view, create, edit, delete
- Permissions: view, create
- Menus: view, create, edit
- Reports: view, export

### Default Menus (7)
1. Dashboard
2. Members
3. Families
4. Membership
5. Matrimony
6. Events
7. Administration

### Default Mappings
- Admin role: all 19 permissions assigned automatically

---

## Integration with Frontend

### 1. Frontend Should Call These APIs:

**Get all roles:**
```
GET /api/v1/roles
```

**Create role:**
```
POST /api/v1/roles
{
  "name": "Treasurer",
  "description": "Manages financial operations"
}
```

**Assign permissions to role:**
```
POST /api/v1/roles/{roleId}/permissions
[1, 2, 3, 4, 5]  // Permission IDs
```

**Get user's permissions:**
```
GET /api/auth/my-permissions
Response: ["users.view", "roles.edit", ...]
```

**Get user's accessible menus:**
```
GET /api/auth/my-menus
Response: Hierarchical menu tree based on permissions
```

---

## Transaction Management

- **Read Operations:** `@Transactional(readOnly = true)`
- **Write Operations:** `@Transactional` (default)
- **Rollback:** Automatic on RuntimeException

---

## Error Handling

### Exception Handling Strategy

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
  
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(
      IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiError("VALIDATION_ERROR", ex.getMessage()));
  }
  
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ApiError("NOT_FOUND", ex.getMessage()));
  }
}
```

---

## Audit Trail Examples

### Role Creation Audit
```json
{
  "action": "CREATE_ROLE",
  "entityType": "ROLE",
  "entityId": 5,
  "userId": 1,
  "changes": ["null", {"name": "Treasurer", "description": "..."}],
  "createdAt": "2026-08-19T10:30:00"
}
```

### Permission Assignment Audit
```json
{
  "action": "ASSIGN_PERMISSION",
  "entityType": "ROLE",
  "entityId": 5,
  "userId": 1,
  "changes": ["null", [1, 2, 3, 4]],
  "createdAt": "2026-08-19T10:35:00"
}
```

---

## Performance Considerations

### Optimizations Implemented

1. **Lazy Loading:** All relationships use `FetchType.LAZY`
2. **Indexes:** Database indexes on frequently queried fields
3. **Pagination:** Audit logs use pagination
4. **Read-Only Transactions:** Read operations explicitly marked as read-only

### Recommended Further Optimizations

1. **Caching:**
   - Cache active roles and permissions
   - Invalidate on updates
   
2. **Batch Operations:**
   - Use batch inserts for bulk permission/role assignments
   
3. **Query Optimization:**
   - Use custom @Query for complex operations
   - Consider EntityGraph for nested loads

---

## Testing Strategy

### Unit Tests Should Cover

1. **RoleService:**
   - Create role validation
   - Update role security checks
   - Permission assignment/removal
   - System role protection

2. **BranchService:**
   - Duplicate code validation
   - Hierarchical branch logic
   - Soft delete behavior

3. **AuditLogService:**
   - JSON serialization
   - Log retrieval by various criteria

### Integration Tests Should Cover

1. **Controller Endpoints:**
   - All CRUD operations
   - Permission-based access control
   - Validation error responses

2. **Database Constraints:**
   - Unique constraints (role names, permission codes)
   - Foreign key constraints
   - Cascade delete behavior

---

## Deployment Checklist

- [ ] Database migration (V2__create_rbac_tables.sql) applied
- [ ] All JPA entities created
- [ ] All repositories created
- [ ] All services created
- [ ] All controllers created
- [ ] Method-level security enabled in SecurityConfig
- [ ] Permission provider configured
- [ ] Audit logging configured
- [ ] Exception handlers added
- [ ] API documentation generated (Swagger/SpringDoc)
- [ ] Unit and integration tests written
- [ ] Performance testing completed

---

## Future Enhancements

1. **Role Inheritance:** Implement role hierarchy (roles inheriting from other roles)
2. **Conditional Permissions:** Time-based, resource-based permissions
3. **Delegation:** Allow users to delegate specific permissions temporarily
4. **Approval Workflows:** Multi-level approval for sensitive role changes
5. **Row-Level Security (RLS):** Control data access at the row level
6. **API Rate Limiting:** Protect admin APIs from abuse
7. **Webhooks:** Notify external systems on RBAC changes
8. **Bulk Import:** CSV import for roles, permissions, users
9. **Activity Dashboard:** Visualize role assignments and permission usage
10. **Permission Analytics:** Report on unused permissions or over-privileged roles

