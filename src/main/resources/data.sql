insert into users(username,password,real_name,role,qualified) select 'admin','1234','系统管理员','ADMIN',true where not exists(select 1 from users where username='admin');
insert into users(username,password,real_name,role,qualified) select 'student','1234','张同学','STUDENT',true where not exists(select 1 from users where username='student');
insert into users(username,password,real_name,role,qualified) select 'teacher','1234','李老师','TEACHER',true where not exists(select 1 from users where username='teacher');
insert into labs(name,location,level,capacity,status,open_time) select '化学基础实验室','理工楼 A203','高风险',24,'开放','周一至周五 08:00-21:00' where not exists(select 1 from labs);
insert into labs(name,location,level,capacity,status,open_time) select '智能制造实验室','创新中心 B108','中风险',18,'开放','周一至周六 09:00-20:00' where not exists(select 1 from labs where name='智能制造实验室');
insert into labs(name,location,level,capacity,status,open_time) select '生物安全实验室','生命楼 C301','高风险',12,'维护中','周一至周五 09:00-18:00' where not exists(select 1 from labs where name='生物安全实验室');
insert into courses(title,category,duration,content,status) select '实验室通用安全基础','通识安全',45,'涵盖个人防护、消防疏散、用水用电和应急报告流程。','已发布' where not exists(select 1 from courses);
insert into courses(title,category,duration,content,status) select '危险化学品规范操作','化学安全',60,'危险化学品分类、领用、存储、废弃物处置和泄漏应急。','已发布' where not exists(select 1 from courses where title='危险化学品规范操作');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '进入实验室前首先应完成什么？','预约场地','安全培训与准入考试','领取设备','登记签到','B',20,'通识安全' where not exists(select 1 from questions);
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '发现化学品泄漏时正确的做法是？','立即徒手清理','隐瞒不报','疏散并按预案报告','继续实验','C',20,'化学安全' where not exists(select 1 from questions where category='化学安全');
insert into equipment(code,name,lab_name,model,status,purchase_date) select 'EQ-2026-001','高效液相色谱仪','化学基础实验室','LC-20A','正常','2025-09-01' where not exists(select 1 from equipment);
insert into equipment(code,name,lab_name,model,status,purchase_date) select 'EQ-2026-002','协作机械臂','智能制造实验室','CR-10','使用中','2026-02-18' where not exists(select 1 from equipment where code='EQ-2026-002');
insert into hazards(name,lab_name,level,protection,emergency) select '易燃有机溶剂','化学基础实验室','重大','防火柜分区存储，操作时佩戴护目镜和耐化学手套。','切断火源，启动通风，使用吸附材料处置并报告。' where not exists(select 1 from hazards);
insert into safety_checks(lab_name,inspector,check_date,result,issue,rectification) select '化学基础实验室','李老师','2026-07-12','限期整改','危化品柜标签轻微破损','已更换标签，待复核' where not exists(select 1 from safety_checks);
insert into notices(title,content,publish_time) select '暑期实验室安全培训安排','进入高风险实验室的同学请于本周完成安全课程与准入考试。','2026-07-15' where not exists(select 1 from notices);
insert into users(username,password,real_name,role,qualified) select '20260101','1234','王思远','STUDENT',true where not exists(select 1 from users where username='20260101');
insert into users(username,password,real_name,role,qualified) select '20260102','1234','陈雨桐','STUDENT',false where not exists(select 1 from users where username='20260102');
insert into users(username,password,real_name,role,qualified) select 't1008','1234','周明远','TEACHER',true where not exists(select 1 from users where username='t1008');
insert into users(username,password,real_name,role,qualified) select 'lab_mgr','1234','赵老师','ADMIN',true where not exists(select 1 from users where username='lab_mgr');
insert into labs(name,location,level,capacity,status,open_time) select '电子电工实验室','工程训练中心 205','中风险',32,'开放','周一至周日 08:30-21:30' where not exists(select 1 from labs where name='电子电工实验室');
insert into labs(name,location,level,capacity,status,open_time) select '材料性能实验室','材料楼 D412','高风险',16,'开放','周一至周五 09:00-18:00' where not exists(select 1 from labs where name='材料性能实验室');
insert into labs(name,location,level,capacity,status,open_time) select '计算机创新实验室','信息楼 E306','低风险',48,'开放','每日 08:00-22:00' where not exists(select 1 from labs where name='计算机创新实验室');
insert into courses(title,category,duration,content,status) select '电气设备与用电安全','电气安全',50,'讲解触电防护、设备接地、漏电保护器使用和电气火灾应急处置。','已发布' where not exists(select 1 from courses where title='电气设备与用电安全');
insert into courses(title,category,duration,content,status) select '实验室消防与应急疏散','消防安全',35,'学习灭火器分类、初起火灾扑救、报警流程和疏散集合要求。','已发布' where not exists(select 1 from courses where title='实验室消防与应急疏散');
insert into courses(title,category,duration,content,status) select '特种设备操作安全','机械安全',70,'覆盖压力容器、高温炉、起重与高速旋转设备的风险辨识和操作规范。','草稿' where not exists(select 1 from courses where title='特种设备操作安全');
insert into study_records(user_name,course_title,progress,completed,study_time) select '张同学','实验室通用安全基础',100,true,'2026-07-13 19:20' where not exists(select 1 from study_records where user_name='张同学' and course_title='实验室通用安全基础');
insert into study_records(user_name,course_title,progress,completed,study_time) select '王思远','危险化学品规范操作',100,true,'2026-07-14 15:36' where not exists(select 1 from study_records where user_name='王思远');
insert into study_records(user_name,course_title,progress,completed,study_time) select '陈雨桐','电气设备与用电安全',65,false,'2026-07-15 10:18' where not exists(select 1 from study_records where user_name='陈雨桐');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '实验室发生电气火灾时应优先采取哪项措施？','用水灭火','切断电源并使用适用灭火器','打开全部门窗','搬走设备','B',20,'电气安全' where not exists(select 1 from questions where category='电气安全');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '使用高速旋转设备前必须检查什么？','设备颜色','防护罩和工件固定情况','房间温度','设备价格','B',20,'机械安全' where not exists(select 1 from questions where category='机械安全');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '实验结束后最后离开人员应当做什么？','直接离开','只关闭照明','检查水电气和门窗并完成登记','清空所有试剂','C',20,'通识安全' where not exists(select 1 from questions where content like '实验结束后%');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '配制或转移易挥发有害溶剂时应在哪里操作？','普通实验台','通风橱内','走廊窗口旁','密闭储物柜内','B',20,'化学安全' where not exists(select 1 from questions where content='配制或转移易挥发有害溶剂时应在哪里操作？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '性质不相容的危险化学品应如何存放？','混合放在同一柜中','按瓶身大小排列','分类分区隔离存放','统一放在地面','C',20,'化学安全' where not exists(select 1 from questions where content='性质不相容的危险化学品应如何存放？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '发现人员触电时，首先应采取什么措施？','直接用手拉开伤员','立即切断电源并确保救援者绝缘','向设备泼水降温','继续观察等待恢复','B',20,'电气安全' where not exists(select 1 from questions where content='发现人员触电时，首先应采取什么措施？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '使用带金属外壳的电气设备前应重点确认什么？','设备品牌','外壳颜色','保护接地可靠','电源线长度','C',20,'电气安全' where not exists(select 1 from questions where content='使用带金属外壳的电气设备前应重点确认什么？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '火灾疏散到集合点后应当做什么？','自行离校','返回实验室取物','清点人员并报告现场负责人','在建筑入口围观','C',20,'消防安全' where not exists(select 1 from questions where content='火灾疏散到集合点后应当做什么？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '使用灭火器扑救初起火灾时，应对准火焰的哪个部位？','顶部','中部','根部','周围空气','C',20,'消防安全' where not exists(select 1 from questions where content='使用灭火器扑救初起火灾时，应对准火焰的哪个部位？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '机械设备运行中出现异常振动或噪声时应如何处理？','提高转速继续观察','立即停机并报告','用手按住振动部件','拆除防护罩检查','B',20,'机械安全' where not exists(select 1 from questions where content='机械设备运行中出现异常振动或噪声时应如何处理？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '取放高温炉内样品时必须使用什么防护用品？','普通棉手套','耐高温手套和面部防护用品','一次性薄膜手套','无需防护用品','B',20,'机械安全' where not exists(select 1 from questions where content='取放高温炉内样品时必须使用什么防护用品？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '发生生物样品洒漏时，正确的处置顺序是什么？','立即用拖把清扫','隔离区域并按生物安全程序消毒处置','用手捡拾后继续实验','打开门窗后离开','B',20,'生物安全' where not exists(select 1 from questions where content='发生生物样品洒漏时，正确的处置顺序是什么？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '进入实验区域时，长发和宽松衣物应如何处理？','保持原样','长发束起并收紧宽松衣物','只佩戴口罩即可','由同伴提醒即可','B',20,'通识安全' where not exists(select 1 from questions where content='进入实验区域时，长发和宽松衣物应如何处理？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '进行激光实验时，最重要的眼部防护措施是什么？','佩戴普通近视眼镜','佩戴与激光波长匹配的防护眼镜','闭上一只眼睛','降低室内照明','B',20,'光学安全' where not exists(select 1 from questions where content='进行激光实验时，最重要的眼部防护措施是什么？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '实验室气瓶应采用什么方式固定？','平放在地面','靠墙放置即可','使用瓶架或链条可靠固定','与热源绑在一起','C',20,'特种设备安全' where not exists(select 1 from questions where content='实验室气瓶应采用什么方式固定？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '实验产生的化学废液应如何处理？','倒入下水道','分类收集、清晰标识并交由规定渠道处置','与生活垃圾混放','长期敞口放置','B',20,'化学安全' where not exists(select 1 from questions where content='实验产生的化学废液应如何处理？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '发生人员受伤或设备事故后应如何报告？','仅在同学群中说明','及时联系指导教师或实验室负责人并按应急流程报告','自行处理且不留记录','实验结束后再决定','B',20,'应急安全' where not exists(select 1 from questions where content='发生人员受伤或设备事故后应如何报告？');
insert into questions(content,option_a,option_b,option_c,option_d,answer,score,category) select '未经培训和授权的人员能否独立操作高风险设备？','可以短时间试用','有同学在场即可','不得独立操作','设备空闲时可以','C',20,'通识安全' where not exists(select 1 from questions where content='未经培训和授权的人员能否独立操作高风险设备？');
insert into exam_records(user_name,score,passed,exam_time) select '张同学',88,true,'2026-07-14 20:15' where not exists(select 1 from exam_records where user_name='张同学');
insert into exam_records(user_name,score,passed,exam_time) select '王思远',92,true,'2026-07-15 09:26' where not exists(select 1 from exam_records where user_name='王思远');
insert into exam_records(user_name,score,passed,exam_time) select '陈雨桐',54,false,'2026-07-15 16:42' where not exists(select 1 from exam_records where user_name='陈雨桐');
insert into qualifications(user_name,lab_name,score,status,valid_until) select '张同学','化学基础实验室',88,'已通过','2027-07-14' where not exists(select 1 from qualifications where user_name='张同学');
insert into qualifications(user_name,lab_name,score,status,valid_until) select '王思远','材料性能实验室',92,'已通过','2027-07-15' where not exists(select 1 from qualifications where user_name='王思远');
insert into qualifications(user_name,lab_name,score,status,valid_until) select '陈雨桐','电子电工实验室',54,'未通过','2026-07-15' where not exists(select 1 from qualifications where user_name='陈雨桐');
insert into reservations(user_id,lab_id,reserve_date,time_slot,purpose,status,reviewer) select u.id,l.id,'2026-07-18','09:00-12:00','分析化学课程实验','已通过','李老师' from users u,labs l where u.username='student' and l.name='化学基础实验室' and not exists(select 1 from reservations where reserve_date='2026-07-18' and time_slot='09:00-12:00');
insert into reservations(user_id,lab_id,reserve_date,time_slot,purpose,status,reviewer) select u.id,l.id,'2026-07-19','14:00-17:00','机器人路径规划课题测试','待审核',null from users u,labs l where u.username='20260101' and l.name='智能制造实验室' and not exists(select 1 from reservations where reserve_date='2026-07-19');
insert into reservations(user_id,lab_id,reserve_date,time_slot,purpose,status,reviewer) select u.id,l.id,'2026-07-20','18:00-21:00','大学生创新项目程序联调','已通过','赵老师' from users u,labs l where u.username='student' and l.name='计算机创新实验室' and not exists(select 1 from reservations where reserve_date='2026-07-20');
insert into equipment(code,name,lab_name,model,status,purchase_date) select 'EQ-2025-018','紫外可见分光光度计','化学基础实验室','UV-2600i','正常','2025-03-12' where not exists(select 1 from equipment where code='EQ-2025-018');
insert into equipment(code,name,lab_name,model,status,purchase_date) select 'EQ-2024-036','万能材料试验机','材料性能实验室','UTM-100KN','正常','2024-10-20' where not exists(select 1 from equipment where code='EQ-2024-036');
insert into equipment(code,name,lab_name,model,status,purchase_date) select 'EQ-2023-107','数字示波器','电子电工实验室','MSO54','维修中','2023-06-08' where not exists(select 1 from equipment where code='EQ-2023-107');
insert into equipment_uses(equipment_name,user_name,use_time,purpose,return_status) select '高效液相色谱仪','王思远','2026-07-15 09:30','样品组分定量分析','已归还' where not exists(select 1 from equipment_uses where equipment_name='高效液相色谱仪');
insert into equipment_uses(equipment_name,user_name,use_time,purpose,return_status) select '协作机械臂','张同学','2026-07-15 14:10','末端执行器抓取测试','使用中' where not exists(select 1 from equipment_uses where equipment_name='协作机械臂');
insert into equipment_uses(equipment_name,user_name,use_time,purpose,return_status) select '数字示波器','陈雨桐','2026-07-12 10:25','电源纹波测量','异常' where not exists(select 1 from equipment_uses where equipment_name='数字示波器');
insert into hazards(name,lab_name,level,protection,emergency) select '高温箱式电阻炉','材料性能实验室','较大','穿戴耐高温手套和面屏，设置高温警示区域。','立即断电，烫伤处用流动冷水处理并联系校医院。' where not exists(select 1 from hazards where name='高温箱式电阻炉');
insert into hazards(name,lab_name,level,protection,emergency) select '工业机械臂运动区域','智能制造实验室','较大','运行时关闭安全围栏，禁止人员进入运动半径。','按下急停按钮，切断动力源并报告设备管理员。' where not exists(select 1 from hazards where name='工业机械臂运动区域');
insert into hazards(name,lab_name,level,protection,emergency) select '高压电源模块','电子电工实验室','重大','双人操作，使用绝缘工具，严格执行断电验电流程。','立即切断上级电源，禁止直接接触伤员并拨打急救电话。' where not exists(select 1 from hazards where name='高压电源模块');
insert into violations(user_name,type,occur_time,handling,status) select '陈雨桐','未按要求佩戴护目镜','2026-07-10 15:20','现场停止实验并完成安全教育补训','已闭环' where not exists(select 1 from violations where user_name='陈雨桐');
insert into violations(user_name,type,occur_time,handling,status) select '王思远','设备使用后未及时登记','2026-07-14 17:45','已补充登记，指导教师提醒谈话','整改中' where not exists(select 1 from violations where user_name='王思远');
insert into violations(user_name,type,occur_time,handling,status) select '张同学','实验区域饮水','2026-07-15 11:05','待实验室负责人处理','待处理' where not exists(select 1 from violations where user_name='张同学');
insert into safety_checks(lab_name,inspector,check_date,result,issue,rectification) select '智能制造实验室','周明远','2026-07-14','正常','未发现明显安全隐患','无需整改' where not exists(select 1 from safety_checks where lab_name='智能制造实验室');
insert into safety_checks(lab_name,inspector,check_date,result,issue,rectification) select '电子电工实验室','赵老师','2026-07-15','限期整改','一台示波器接地端口松动','设备已停用并提交维修工单' where not exists(select 1 from safety_checks where lab_name='电子电工实验室');
insert into safety_checks(lab_name,inspector,check_date,result,issue,rectification) select '计算机创新实验室','李老师','2026-07-15','正常','疏散通道和消防设施状态正常','无需整改' where not exists(select 1 from safety_checks where lab_name='计算机创新实验室');
insert into notices(title,content,publish_time) select '化学基础实验室通风系统检修通知','7月21日 13:00 至 17:00 开展通风系统维护，期间暂停预约和实验活动。','2026-07-15' where not exists(select 1 from notices where title like '化学基础%');
insert into notices(title,content,publish_time) select '2026 年暑期开放实验室值班安排','暑期开放实验室实行教师值班制度，请预约人员按审批时段入场。','2026-07-14' where not exists(select 1 from notices where title like '2026 年暑期%');
insert into notices(title,content,publish_time) select '高温天气实验安全提示','近期持续高温，请加强气瓶、危化品和高温设备检查，实验结束后确认水电气关闭。','2026-07-13' where not exists(select 1 from notices where title like '高温天气%');
insert into operation_logs(operator_name,action,module,operate_time,detail) select '李老师','预约审核','预约管理','2026-07-15 09:42','通过化学基础实验室预约申请' where not exists(select 1 from operation_logs where detail='通过化学基础实验室预约申请');
insert into operation_logs(operator_name,action,module,operate_time,detail) select '赵老师','更新设备状态','设备管理','2026-07-15 11:18','数字示波器状态调整为维修中' where not exists(select 1 from operation_logs where detail='数字示波器状态调整为维修中');
insert into operation_logs(operator_name,action,module,operate_time,detail) select '系统管理员','发布公告','系统管理','2026-07-15 14:30','发布实验室通风系统检修通知' where not exists(select 1 from operation_logs where detail='发布实验室通风系统检修通知');

update courses set content = concat(
'## 学习目标', char(10),
'完成本课程后，学习人员应能够识别实验室常见风险，正确使用个人防护用品，并掌握事故报告和疏散的基本流程。', char(10),
'- 了解进入实验室前必须完成的培训、考试、资格和预约要求。', char(10),
'- 能够根据实验类型选择实验服、护目镜、手套等防护用品。', char(10),
'- 知道紧急情况下应先保护人员安全，再报告和处置。', char(10),
'## 入场前检查', char(10),
'进入实验室前应确认本人准入资格和预约时段有效，阅读实验方案与风险告知，熟悉安全出口、洗眼器、灭火器和紧急断电装置的位置。', char(10),
'- 禁止穿拖鞋、短裤或其他不符合防护要求的服装进入实验区域。', char(10),
'- 不得携带食品饮料，不得在实验区域追逐、嬉闹或单独开展高风险操作。', char(10),
'> 对不熟悉的设备、试剂和操作步骤，必须先向指导教师确认，不得凭经验试用。', char(10),
'## 实验过程规范', char(10),
'实验过程中应严格执行操作规程，保持台面整洁，及时标识样品和废弃物。使用水、电、气和热源时不得擅自离岗。', char(10),
'- 发现异味、异响、泄漏、冒烟或人员不适时立即停止操作。', char(10),
'- 实验结束后关闭水电气源，清理现场并完成设备或实验记录。', char(10),
'## 应急报告与疏散', char(10),
'发生事故时应立即示警，按照现场预案切断危险源并组织人员撤离，在确保自身安全的前提下实施初期处置。', char(10),
'- 报告时说明实验室位置、事故类型、涉及物质、人员受伤和现场控制情况。', char(10),
'- 到达集合点后清点人员，未经允许不得返回事故区域。', char(10),
'> 安全优先于实验进度。任何人发现重大风险都有权立即停止实验并报告。')
where title = '实验室通用安全基础' and content = '涵盖个人防护、消防疏散、用水用电和应急报告流程。';

update courses set content = concat(
'## 学习目标', char(10),
'本课程帮助学习人员掌握危险化学品从领取、使用、暂存到废弃处置的全过程要求，并能够在泄漏、灼伤和火灾发生时采取正确措施。', char(10),
'- 能够阅读化学品标签和安全技术说明书，识别主要危险性。', char(10),
'- 掌握分类存放、规范取用、废液收集和异常报告要求。', char(10),
'## 领用与储存', char(10),
'领用前应核对名称、浓度、数量和容器状态，确认实验方案规定的最小使用量。易燃、氧化、酸碱和有毒化学品必须按相容性分区存放。', char(10),
'- 所有试剂容器必须具有清晰标签，严禁使用饮料瓶等非专用容器盛装。', char(10),
'- 易制毒、易制爆和剧毒品按学校制度执行双人领用、双人保管和台账登记。', char(10),
'> 通风橱不是储物柜，实验结束后应将试剂归还指定位置。', char(10),
'## 规范操作与废弃处置', char(10),
'涉及挥发性、腐蚀性或有毒物质的操作应在有效通风设施内完成。转移液体时使用合适工具，禁止用口吸取，禁止将未知物质直接混合。', char(10),
'- 按类别收集有机、无机、含重金属和锐器废弃物，不得直接排入下水道。', char(10),
'- 废液桶达到规定容量后及时封闭、标识并申请转运。', char(10),
'## 泄漏与人员暴露', char(10),
'发现泄漏后立即停止作业、隔离现场并报告。只有经过培训且防护条件满足的人员才能按预案进行小规模泄漏处置。', char(10),
'- 皮肤或眼睛接触化学品时立即使用大量流动清水冲洗，并携带物质信息就医。', char(10),
'- 发生大范围泄漏、火灾或不明气体释放时，应立即疏散并联系学校应急力量。', char(10),
'> 不得徒手处理泄漏物，也不得在不了解物质性质时使用吸附剂或清洗剂。')
where title = '危险化学品规范操作' and content = '危险化学品分类、领用、存储、废弃物处置和泄漏应急。';

update courses set content = concat(
'## 学习目标', char(10),
'完成本课程后，学习人员应能够识别触电、电弧和电气火灾风险，正确进行设备接线、通电前检查和异常断电处置。', char(10),
'- 理解保护接地、漏电保护和安全电压的基本作用。', char(10),
'- 掌握停电、验电、挂牌和恢复供电的操作顺序。', char(10),
'## 通电前检查', char(10),
'使用电气设备前应检查电源线、插头、绝缘、接地和防护罩是否完好，确认设备额定电压与供电条件相符。', char(10),
'- 禁止湿手操作开关，禁止私拉电线或使用破损插排。', char(10),
'- 高压、大功率或裸露导体实验必须设置隔离区域并由两人配合。', char(10),
'> 接线和调整电路必须在断电状态下进行，通电前由指导人员复核。', char(10),
'## 运行与检修', char(10),
'设备运行期间应观察温升、气味、声音和仪表读数。出现跳闸、冒烟、异常发热时立即断电，不得反复强行合闸。', char(10),
'- 检修前执行停电、验电和防止误送电措施。', char(10),
'- 电容器、高压模块等可能残留能量的部件必须按规程放电。', char(10),
'## 触电与电气火灾应急', char(10),
'人员触电时先切断电源或使用绝缘工具使其脱离电源，禁止直接徒手接触。确认现场安全后立即呼叫急救并实施必要救护。', char(10),
'- 电气火灾应先断电，再使用适用的灭火器材，严禁带电用水扑救。', char(10),
'- 事故设备必须隔离并保留现场信息，未经检查不得重新启用。', char(10),
'> 救援人员首先确保自身绝缘和现场断电，避免发生二次伤害。')
where title = '电气设备与用电安全' and content = '讲解触电防护、设备接地、漏电保护器使用和电气火灾应急处置。';

update courses set content = concat(
'## 学习目标', char(10),
'本课程使学习人员熟悉实验室火灾预防、报警、初期扑救和人员疏散流程，能够根据燃烧物类型选择适用灭火器材。', char(10),
'- 识别常见火源、可燃物和消防通道管理要求。', char(10),
'- 掌握报警信息、疏散路线和集合清点要求。', char(10),
'## 火灾预防', char(10),
'保持疏散通道、安全出口和消防设施前方畅通。易燃物远离热源和电气设备，动火作业必须履行审批并落实现场监护。', char(10),
'- 实验结束后检查电热设备、气源和通风系统是否按要求关闭。', char(10),
'- 灭火器、消火栓和报警按钮不得遮挡、挪用或随意拆卸。', char(10),
'## 报警与初期处置', char(10),
'发现火情立即示警并报告，说明准确位置、燃烧物、火势、被困人员和危险化学品情况。火势较小时可在保持退路的前提下使用适用器材处置。', char(10),
'- 可燃液体、电气设备和金属火灾必须选用相匹配的灭火介质。', char(10),
'- 浓烟增大、火势扩展或情况不明时立即撤离，不得冒险扑救。', char(10),
'> 初期灭火必须始终保持面向出口的撤离路线，无法控制时迅速关门撤离。', char(10),
'## 疏散与集合', char(10),
'疏散时保持低姿，按照指示路线有序撤离，不乘坐电梯，不返回取物。协助行动不便人员并在指定地点集合。', char(10),
'- 到达集合点后由负责人清点人员并向应急人员报告。', char(10),
'- 未经现场指挥人员允许，任何人不得返回建筑物。')
where title = '实验室消防与应急疏散' and content = '学习灭火器分类、初起火灾扑救、报警流程和疏散集合要求。';

update courses set content = concat(
'## 学习目标', char(10),
'学习压力容器、高温炉、起重和高速旋转设备的主要风险及授权操作要求。', char(10),
'- 理解设备点检、操作许可和双人作业制度。', char(10),
'## 操作前确认', char(10),
'核对设备状态、检验标识、防护装置和应急停止功能，确认工件、夹具和附件固定可靠。', char(10),
'- 未经培训授权不得启动特种设备。', char(10),
'- 防护罩、联锁和限位装置不得拆除或旁路。', char(10),
'## 运行监控', char(10),
'运行时人员不得进入危险运动区域，不得触摸高温、旋转或承压部件。发现异常振动、压力、温度和噪声时立即停机。', char(10),
'## 异常处置', char(10),
'按下急停并切断动力源，设置隔离标识并报告设备管理员。故障原因未查明、保护装置未恢复前不得重新启动。', char(10),
'> 特种设备事故处置应服从现场负责人指挥，避免盲目泄压、开盖或接触运动部件。')
where title = '特种设备操作安全' and content = '覆盖压力容器、高温炉、起重与高速旋转设备的风险辨识和操作规范。';
