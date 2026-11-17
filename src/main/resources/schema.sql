create table if not exists users (
    id bigint not null auto_increment,
    username varchar(50) not null,
    password varchar(100) not null,
    real_name varchar(50) not null,
    role varchar(20) not null,
    qualified boolean not null default false,
    primary key (id),
    unique key uk_users_username (username),
    key idx_users_real_name (real_name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists labs (
    id bigint not null auto_increment,
    name varchar(100) not null,
    location varchar(100) not null,
    level varchar(20) not null,
    capacity int not null,
    status varchar(20) not null,
    open_time varchar(100) not null,
    primary key (id),
    unique key uk_labs_name (name),
    key idx_labs_status (status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists reservations (
    id bigint not null auto_increment,
    user_id bigint not null,
    lab_id bigint not null,
    reserve_date varchar(20) not null,
    time_slot varchar(50) not null,
    purpose varchar(255) not null,
    status varchar(20) not null,
    reviewer varchar(50),
    primary key (id),
    key idx_reservation_user (user_id),
    key idx_reservation_slot (lab_id, reserve_date, time_slot, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists courses (
    id bigint not null auto_increment,
    title varchar(150) not null,
    category varchar(50) not null,
    duration int not null,
    content text not null,
    status varchar(20) not null,
    primary key (id),
    unique key uk_courses_title (title)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists study_records (
    id bigint not null auto_increment,
    user_name varchar(50) not null,
    course_title varchar(150) not null,
    progress int not null,
    completed boolean not null,
    study_time varchar(30) not null,
    primary key (id),
    unique key uk_study_user_course (user_name, course_title)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists questions (
    id bigint not null auto_increment,
    content varchar(500) not null,
    option_a varchar(200) not null,
    option_b varchar(200) not null,
    option_c varchar(200) not null,
    option_d varchar(200) not null,
    answer varchar(10) not null,
    score int not null,
    category varchar(50) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists exam_records (
    id bigint not null auto_increment,
    user_name varchar(50) not null,
    score int not null,
    passed boolean not null,
    exam_time varchar(30) not null,
    primary key (id),
    key idx_exam_user (user_name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists qualifications (
    id bigint not null auto_increment,
    user_name varchar(50) not null,
    lab_name varchar(100) not null,
    score int not null,
    status varchar(20) not null,
    valid_until varchar(20) not null,
    primary key (id),
    key idx_qualification_user_status (user_name, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists equipment (
    id bigint not null auto_increment,
    code varchar(50) not null,
    name varchar(100) not null,
    lab_name varchar(100) not null,
    model varchar(100) not null,
    status varchar(20) not null,
    purchase_date varchar(20) not null,
    primary key (id),
    unique key uk_equipment_code (code),
    key idx_equipment_lab (lab_name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists equipment_uses (
    id bigint not null auto_increment,
    equipment_name varchar(100) not null,
    user_name varchar(50) not null,
    use_time varchar(30) not null,
    purpose varchar(255) not null,
    return_status varchar(30) not null,
    primary key (id),
    key idx_equipment_use_name (equipment_name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists hazards (
    id bigint not null auto_increment,
    name varchar(100) not null,
    lab_name varchar(100) not null,
    level varchar(20) not null,
    protection varchar(500) not null,
    emergency varchar(500) not null,
    primary key (id),
    key idx_hazards_lab (lab_name)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists violations (
    id bigint not null auto_increment,
    user_name varchar(50) not null,
    type varchar(100) not null,
    occur_time varchar(30) not null,
    handling varchar(500) not null,
    status varchar(20) not null,
    primary key (id),
    key idx_violations_status (status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists safety_checks (
    id bigint not null auto_increment,
    lab_name varchar(100) not null,
    inspector varchar(50) not null,
    check_date varchar(20) not null,
    result varchar(20) not null,
    issue varchar(500),
    rectification varchar(500),
    primary key (id),
    key idx_checks_lab_date (lab_name, check_date)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists notices (
    id bigint not null auto_increment,
    title varchar(200) not null,
    content varchar(1000) not null,
    publish_time varchar(30) not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists operation_logs (
    id bigint not null auto_increment,
    operator_name varchar(50) not null,
    action varchar(100) not null,
    module varchar(100) not null,
    operate_time varchar(30) not null,
    detail varchar(500) not null,
    primary key (id),
    key idx_logs_time (operate_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
