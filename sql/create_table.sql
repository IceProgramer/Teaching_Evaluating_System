-- 创建表
create database if not exists eva;

-- 使用表
use eva;

-- 管理员表
create table e_admin
(
    id          int auto_increment comment '用户名'
        primary key,
    username    varchar(256)      not null comment '账号',
    password    varchar(256)      not null comment '密码',
    role        tinyint default 1 not null comment '权限',
    addressIp   varchar(256)      null comment '最近一次登录的ip地址',
    addressName varchar(256)      null comment '形式:中国-省份-城市'
)
    comment '管理员表';

-- 教师表
create table e_teacher
(
    id       bigint auto_increment comment '主键'
        primary key,
    name     varchar(256) not null comment '教师名称',
    sex      tinyint      not null comment '性别',
    age      int          not null comment '年龄',
    position int          not null comment '职称',
    title    int          not null comment '职称',
    major    varchar(256) not null comment '专业（0-计算机，1-自动化）',
    email    varchar(512) not null comment '邮箱',
    identity tinyint      not null comment '国籍（0-俄罗斯，1-中国）'
)
    comment '教师表';


-- 学生表
create table e_student
(
    id          bigint auto_increment comment '主键'
        primary key,
    sid         varchar(256)  not null comment '学号',
    password    varchar(256)  null comment '密码',
    name        varchar(256)  not null comment '姓名',
    sex         tinyint       not null comment '性别(0-女 1-男）',
    age         int           not null comment '年龄',
    major       tinyint       not null comment '专业（0-计算机，1-自动化）',
    cid         int           not null comment '班级id',
    grade       int           not null comment '年级',
    tag         varchar(1024) null comment '用户名',
    addressIp   varchar(256)  null comment '最近登录的一次ip地址',
    addressName varchar(256)  null comment '形式:中国-省份-城市'
)
    comment '学生表';


-- 职位表
create table if not exists eva.`e_position`
(
    `id` int not null auto_increment comment '主键' primary key,
    `name` varchar(256) not null comment '职位名称'
) comment '职位表';

-- 职称表
create table if not exists eva.`e_title`
(
    `id` int not null auto_increment comment '主键' primary key,
    `name` varchar(256) not null comment '职称名称'
) comment '职称表';

-- 班级表
create table if not exists eva.`e_class`
(
    `id` int not null auto_increment comment '主键' primary key,
    `cid` varchar(256) not null comment '班级号'
) comment '班级表';

-- 课程表
create table if not exists eva.`e_course`
(
    `id` int not null auto_increment comment '主键' primary key,
    `cName` varchar(256) not null comment '课程中文名',
    `eName` varchar(256) not null comment '课程中文名',
    `major` varchar(256) not null comment '专业',
    `tid` int not null comment '教师id',
    `grade` int null comment '年级'
) comment '课程表';

-- 评测表
create table e_evaluate
(
    id          int auto_increment
        primary key,
    name        varchar(200) null comment '评测名称',
    create_time varchar(200) null comment '创建时间',
    start_time  varchar(200) null comment '开始时间',
    e_time      varchar(200) null comment '结束时间',
    status      int          null comment '发布状态（0-评测结束 1-正在评测）'
) comment '评测表';

-- 评价体系表
create table if not exists eva.`e_system`
(
    `id` int not null auto_increment comment '主键' primary key,
    `name` varchar(256) not null comment '指标名称',
    `level` int not null comment '评价级别',
    `kind` int not null comment '0为俄方，1为中方',
    `sid` int not null comment '二级指标指向一级指标'
) comment '评价体系表';