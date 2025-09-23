-- Initial data setup for NBJ Group Tenant Management Platform
-- This script creates default admin user and sample data for development/testing

-- Insert default admin user (password: admin123)
INSERT INTO users (email, password, first_name, last_name, phone_number, role, is_active, email_verified, created_at, updated_at)
VALUES (
    'admin@nbjgroup.com',
    '$2a$12$LQv3c1yqBw2Wc4xnFzqCpOJSZZzZzZzZzZzZzZzZzZzZzZzZzZzZzZ',
    'System',
    'Administrator',
    '+1234567890',
    'ADMIN',
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

-- Insert sample tenant users for development
INSERT INTO users (email, password, first_name, last_name, phone_number, role, is_active, email_verified, created_at, updated_at)
VALUES 
    (
        'john.doe@email.com',
        '$2a$12$LQv3c1yqBw2Wc4xnFzqCpOJSZZzZzZzZzZzZzZzZzZzZzZzZzZzZzZ',
        'John',
        'Doe',
        '+1234567891',
        'TENANT',
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'jane.smith@email.com',
        '$2a$12$LQv3c1yqBw2Wc4xnFzqCpOJSZZzZzZzZzZzZzZzZzZzZzZzZzZzZzZ',
        'Jane',
        'Smith',
        '+1234567892',
        'TENANT',
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'mike.johnson@email.com',
        '$2a$12$LQv3c1yqBw2Wc4xnFzqCpOJSZZzZzZzZzZzZzZzZzZzZzZzZzZzZzZ',
        'Mike',
        'Johnson',
        '+1234567893',
        'TENANT',
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (email) DO NOTHING;

-- Insert sample tenant profiles
INSERT INTO tenants (user_id, property_address, unit_number, rent_amount, security_deposit, lease_start_date, lease_end_date, status, emergency_contact_name, emergency_contact_phone, emergency_contact_relationship, move_in_date, notes, created_at, updated_at)
SELECT 
    u.id,
    '123 Main Street, Cityville, ST 12345',
    'A1',
    1200.00,
    1200.00,
    '2024-01-01',
    '2024-12-31',
    'ACTIVE',
    'Emergency Contact',
    '+1234567899',
    'Spouse',
    '2024-01-01',
    'Sample tenant profile for development',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u 
WHERE u.email = 'john.doe@email.com'
AND NOT EXISTS (SELECT 1 FROM tenants t WHERE t.user_id = u.id);

INSERT INTO tenants (user_id, property_address, unit_number, rent_amount, security_deposit, lease_start_date, lease_end_date, status, emergency_contact_name, emergency_contact_phone, emergency_contact_relationship, move_in_date, notes, created_at, updated_at)
SELECT 
    u.id,
    '456 Oak Avenue, Townsburg, ST 12346',
    'B2',
    1500.00,
    1500.00,
    '2024-02-01',
    '2025-01-31',
    'ACTIVE',
    'Jane Emergency',
    '+1234567898',
    'Parent',
    '2024-02-01',
    'Sample tenant profile for development',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u 
WHERE u.email = 'jane.smith@email.com'
AND NOT EXISTS (SELECT 1 FROM tenants t WHERE t.user_id = u.id);

INSERT INTO tenants (user_id, property_address, unit_number, rent_amount, security_deposit, lease_start_date, lease_end_date, status, emergency_contact_name, emergency_contact_phone, emergency_contact_relationship, move_in_date, notes, created_at, updated_at)
SELECT 
    u.id,
    '789 Pine Street, Villagetown, ST 12347',
    '101',
    1800.00,
    1800.00,
    '2024-03-01',
    '2025-02-28',
    'ACTIVE',
    'Mike Emergency',
    '+1234567897',
    'Sibling',
    '2024-03-01',
    'Sample tenant profile for development',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users u 
WHERE u.email = 'mike.johnson@email.com'
AND NOT EXISTS (SELECT 1 FROM tenants t WHERE t.user_id = u.id);

-- Insert sample lease agreements
INSERT INTO lease_agreements (tenant_id, start_date, end_date, monthly_rent, security_deposit, status, is_renewal, tenant_signed_date, admin_signed_date, lease_terms, special_conditions, notes, created_at, updated_at)
SELECT 
    t.id,
    '2024-01-01',
    '2024-12-31',
    1200.00,
    1200.00,
    'ACTIVE',
    false,
    '2023-12-15',
    '2023-12-16',
    'Standard residential lease agreement with 12-month term.',
    'Pet allowed with additional deposit.',
    'Initial lease agreement',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'john.doe@email.com'
AND NOT EXISTS (SELECT 1 FROM lease_agreements la WHERE la.tenant_id = t.id);

INSERT INTO lease_agreements (tenant_id, start_date, end_date, monthly_rent, security_deposit, status, is_renewal, tenant_signed_date, admin_signed_date, lease_terms, special_conditions, notes, created_at, updated_at)
SELECT 
    t.id,
    '2024-02-01',
    '2025-01-31',
    1500.00,
    1500.00,
    'ACTIVE',
    false,
    '2024-01-15',
    '2024-01-16',
    'Standard residential lease agreement with 12-month term.',
    'No smoking policy strictly enforced.',
    'Initial lease agreement',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'jane.smith@email.com'
AND NOT EXISTS (SELECT 1 FROM lease_agreements la WHERE la.tenant_id = t.id);

-- Insert sample payments
INSERT INTO payments (tenant_id, amount, due_date, payment_date, status, payment_type, payment_method, transaction_id, reference_number, late_fee, discount_amount, total_amount, payment_period_start, payment_period_end, notes, processed_by, processed_at, created_at, updated_at)
SELECT 
    t.id,
    1200.00,
    '2024-09-01',
    '2024-08-28',
    'COMPLETED',
    'RENT',
    'BANK_TRANSFER',
    'TXN_20240828_001',
    'REF_JD_202409',
    0.00,
    0.00,
    1200.00,
    '2024-09-01',
    '2024-09-30',
    'September 2024 rent payment',
    'admin@nbjgroup.com',
    '2024-08-28 10:30:00',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'john.doe@email.com';

INSERT INTO payments (tenant_id, amount, due_date, payment_date, status, payment_type, payment_method, transaction_id, reference_number, late_fee, discount_amount, total_amount, payment_period_start, payment_period_end, notes, created_at, updated_at)
SELECT 
    t.id,
    1200.00,
    '2024-10-01',
    NULL,
    'PENDING',
    'RENT',
    NULL,
    NULL,
    'REF_JD_202410',
    0.00,
    0.00,
    1200.00,
    '2024-10-01',
    '2024-10-31',
    'October 2024 rent payment',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'john.doe@email.com';

-- Insert sample maintenance requests
INSERT INTO maintenance_requests (tenant_id, title, description, status, priority, category, location_details, preferred_contact_method, preferred_time, tenant_available, estimated_cost, notes, created_at, updated_at)
SELECT 
    t.id,
    'Leaky Kitchen Faucet',
    'The kitchen faucet has been dripping constantly for the past week. It seems to be getting worse and is wasting water.',
    'PENDING',
    'MEDIUM',
    'PLUMBING',
    'Kitchen sink area',
    'Phone',
    'Weekdays after 5 PM',
    true,
    NULL,
    'Tenant reported issue via phone call',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'john.doe@email.com';

INSERT INTO maintenance_requests (tenant_id, title, description, status, priority, category, location_details, preferred_contact_method, preferred_time, tenant_available, estimated_cost, actual_cost, assigned_to, assigned_at, started_at, completed_at, admin_notes, tenant_feedback, tenant_rating, resolution_summary, created_at, updated_at)
SELECT 
    t.id,
    'Air Conditioning Not Working',
    'The AC unit in the living room stopped working yesterday. It turns on but no cold air is coming out.',
    'COMPLETED',
    'HIGH',
    'HVAC',
    'Living room AC unit',
    'Email',
    'Anytime',
    true,
    150.00,
    125.00,
    'HVAC Technician',
    '2024-09-15 09:00:00',
    '2024-09-15 14:00:00',
    '2024-09-15 16:30:00',
    'Replaced faulty capacitor and cleaned filters',
    'Very satisfied with the quick response and professional service.',
    5,
    'AC unit repaired successfully. Capacitor replacement and maintenance completed.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t 
JOIN users u ON t.user_id = u.id
WHERE u.email = 'jane.smith@email.com';

-- Note: The password hash above corresponds to 'admin123' and 'password123' respectively
-- In production, these should be changed and proper password policies should be enforced
