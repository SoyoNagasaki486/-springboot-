const select = (options) => ({ type: 'select', options })

export const resourceConfigs = {
  users: {
    title: '用户与权限',
    description: '维护学生、教师和管理员账号，分配系统角色与准入状态',
    editRoles: ['ADMIN'],
    fields: [
      { key: 'username', label: '账号' },
      { key: 'password', label: '密码', type: 'password', table: false, optionalOnEdit: true, minlength: 8, maxlength: 72 },
      { key: 'real_name', label: '姓名' },
      { key: 'role', label: '角色', ...select([['STUDENT', '学生'], ['TEACHER', '教师'], ['ADMIN', '管理员']]) },
      { key: 'qualified', label: '准入资格', boolean: true, form: false },
    ],
  },
  labs: {
    title: '实验室信息',
    description: '管理开放时间、容纳人数、安全等级和运行状态',
    editRoles: ['ADMIN'],
    fields: [
      { key: 'name', label: '实验室名称' }, { key: 'location', label: '所在位置' },
      { key: 'level', label: '风险等级', ...select(['低风险', '中风险', '高风险']) },
      { key: 'capacity', label: '容纳人数', type: 'number' },
      { key: 'status', label: '状态', ...select(['开放', '维护中', '停用', '停用整改']) },
      { key: 'open_time', label: '开放时间' },
    ],
  },
  courses: {
    title: '安全培训课程',
    description: '发布安全制度、操作规范、事故案例和应急知识课程',
    editRoles: ['ADMIN'],
    fields: [
      { key: 'title', label: '课程名称' }, { key: 'category', label: '安全类别' },
      { key: 'duration', label: '时长（分钟）', type: 'number' },
      { key: 'status', label: '发布状态', ...select(['已发布', '草稿']) },
      {
        key: 'content', label: '课程内容', type: 'textarea', full: true,
        help: '使用“## 章节标题”划分章节，“- ”填写学习要点，“> ”填写安全重点；发布时至少 3 章、200 字。',
        placeholder: '## 学习目标\n课程导学正文……\n- 学习要点一\n- 学习要点二\n## 操作规范\n……\n## 应急处置\n……',
      },
    ],
  },
  'study-records': {
    title: '学习记录', description: '查看课程学习进度、完成状态和最近学习时间', readonly: true,
    fields: [
      { key: 'user_name', label: '学习人员' }, { key: 'course_title', label: '课程' },
      { key: 'progress', label: '学习进度', progress: true },
      { key: 'completed', label: '完成状态', boolean: true }, { key: 'study_time', label: '最近学习' },
    ],
  },
  questions: {
    title: '安全考试题库', description: '维护准入考试题目、选项、正确答案和分值', editRoles: ['ADMIN'],
    fields: [
      { key: 'content', label: '题目内容', type: 'textarea', full: true }, { key: 'category', label: '安全类别' },
      { key: 'option_a', label: '选项 A' }, { key: 'option_b', label: '选项 B' },
      { key: 'option_c', label: '选项 C' }, { key: 'option_d', label: '选项 D' },
      { key: 'answer', label: '正确答案', ...select(['A', 'B', 'C', 'D']) },
      { key: 'score', label: '分值', type: 'number' },
    ],
  },
  qualifications: {
    title: '准入资格审核', description: '结合培训完成情况与考试成绩确认适用实验室和有效期', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'user_name', label: '用户姓名' }, { key: 'lab_name', label: '适用实验室' },
      { key: 'score', label: '考试成绩', type: 'number' },
      { key: 'status', label: '审核状态', ...select(['待审核', '已通过', '未通过']) },
      { key: 'valid_until', label: '有效期至', type: 'date' },
    ],
  },
  equipment: {
    title: '设备台账', description: '管理仪器设备归属、型号、采购时间和运行状态', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'code', label: '设备编号' }, { key: 'name', label: '设备名称' },
      { key: 'lab_name', label: '所属实验室' }, { key: 'model', label: '设备型号' },
      { key: 'status', label: '运行状态', ...select(['正常', '使用中', '维修中', '停用']) },
      { key: 'purchase_date', label: '购置日期', type: 'date' },
    ],
  },
  'equipment-uses': {
    title: '设备使用登记', description: '记录使用人员、时间、用途和归还情况，确保过程可追溯', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'equipment_name', label: '设备名称' }, { key: 'user_name', label: '使用人' },
      { key: 'use_time', label: '使用时间', type: 'datetime-local' },
      { key: 'purpose', label: '使用目的', full: true },
      { key: 'return_status', label: '归还情况', ...select(['使用中', '已归还', '异常']) },
    ],
  },
  hazards: {
    title: '危险源管理', description: '登记危险化学品、高压高温设备及防护和应急措施', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'name', label: '危险源名称' }, { key: 'lab_name', label: '所在实验室' },
      { key: 'level', label: '危险等级', ...select(['一般', '较大', '重大']) },
      { key: 'protection', label: '防护要求', type: 'textarea', full: true },
      { key: 'emergency', label: '应急措施', type: 'textarea', full: true },
    ],
  },
  violations: {
    title: '违规行为记录', description: '记录违规类型、处理结果和整改状态，为安全教育提供依据', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'user_name', label: '责任人' }, { key: 'type', label: '违规类型' },
      { key: 'occur_time', label: '发生时间', type: 'datetime-local' },
      { key: 'handling', label: '处理结果', type: 'textarea', full: true },
      { key: 'status', label: '状态', ...select(['待处理', '整改中', '已闭环']) },
    ],
  },
  checks: {
    title: '安全检查', description: '跟踪日常检查、发现问题和整改闭环情况', editRoles: ['ADMIN', 'TEACHER'],
    fields: [
      { key: 'lab_name', label: '实验室' }, { key: 'inspector', label: '检查人' },
      { key: 'check_date', label: '检查日期', type: 'date' },
      { key: 'result', label: '检查结果', ...select(['正常', '限期整改', '停用整改']) },
      { key: 'issue', label: '发现问题', type: 'textarea', full: true },
      { key: 'rectification', label: '整改结果', type: 'textarea', full: true },
    ],
  },
  notices: {
    title: '通知公告', description: '发布开放调整、培训考试、设备维护和预约变更通知', editRoles: ['ADMIN'],
    fields: [
      { key: 'title', label: '公告标题' }, { key: 'publish_time', label: '发布日期', type: 'date' },
      { key: 'content', label: '公告内容', type: 'textarea', full: true },
    ],
  },
  logs: {
    title: '操作日志', description: '查询登录、数据维护和业务审核记录，支持责任追溯', readonly: true,
    fields: [
      { key: 'operator_name', label: '操作人' }, { key: 'action', label: '操作' },
      { key: 'module', label: '模块' }, { key: 'operate_time', label: '时间' }, { key: 'detail', label: '详情' },
    ],
  },
}

export function normalizeForm(config, form) {
  return Object.fromEntries(config.fields.filter((field) => field.form !== false).map((field) => {
    let value = form[field.key]
    if (field.boolean) value = value === true || value === 'true'
    if (field.type === 'number' && value !== '') value = Number(value)
    if (field.type === 'datetime-local') value = String(value || '').replace('T', ' ')
    return [field.key, value]
  }).filter(([, value]) => value !== undefined))
}
