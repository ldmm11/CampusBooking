use cbooking;

# 建表
# 用户表
create table if not exists t_user(
    id bigint primary key auto_increment,
    name varchar(16) not null,
    password varchar(100) not null,
    nickname varchar(16) not null default '某某' comment '昵称',
    role int not null default 0 comment '身份0学生,1超级管理员,2场馆管理员,3游客',
    banned_until datetime null comment '禁约截止',
    create_at datetime,
    update_at datetime
)comment '学生表';

# 场馆表
create table if not exists t_venue(
    id bigint primary key auto_increment,
    name varchar(64) not null,
    type int not null comment '类别0 Badminton 1 Study_Room 2 Basketball',
    location varchar(128) not null default'',
    capacity int not null default 1,
    open_time time not null,
    close_time time not null,
    status tinyint null default 1 comment '状态1启用 0停用',
    create_at datetime,
    update_at datetime
)comment '场馆表';

# 时段表
create table if not exists t_slot(
    id bigint primary key auto_increment,
    venue_id bigint not null,
    slot_date date not null,
    start_time time not null,
    end_time time not null,
    status tinyint not null default 0 comment '状态 0可预约 1已占用 2已关闭',
    create_at datetime,
    update_at datetime
)comment '时段表';

#预约表
create table if not exists t_booking(
    id bigint primary key auto_increment,
    booking_no varchar(32) not null ,
    user_id bigint not null,
    venue_id bigint not null,
    slot_id bigint not null references t_slot(id),
    amount decimal(10,2) not null default 0 comment '支付金额',
    expire_at datetime not null comment '到期',
    status tinyint not null default 0
        comment '0待支付 1已确认 2已签到 3已取消 4已失效 5未到场',
    pay_at datetime null,
    check_in datetime null,
    cancel_at datetime null,
    no_show_at datetime null,
    create_at datetime,
    update_at datetime
)comment '预约表';

#索引
-- 用户表：登录靠 name 查
alter table t_user
    add unique key uk_name (name);

-- 场馆表：列表页按类型 + 状态筛
alter table t_venue
    add key idx_type_status (type, status);

-- 时段表
alter table t_slot
    add unique key uk_venue_date_start (venue_id, slot_date, start_time),
    add key idx_date_status (slot_date, status);

-- 预约表
alter table t_booking
    add unique key uk_booking_no (booking_no),
    add key idx_user_status (user_id, status),
    add key idx_slot (slot_id),
    add key idx_status_expire (status, expire_at);
