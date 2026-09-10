-- 城市社区适老化改造入户评估与施工交付平台 初始 schema

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL UNIQUE,
    password        VARCHAR(128) NOT NULL,
    real_name       VARCHAR(64)  NOT NULL,
    role            VARCHAR(24)  NOT NULL,
    phone           VARCHAR(32),
    organization    VARCHAR(128),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- 申请（老人/家属提交，社区核验、派单）
CREATE TABLE application (
    id                      BIGSERIAL PRIMARY KEY,
    applicant_name          VARCHAR(64)  NOT NULL,   -- 申请人（老人或家属）
    elder_name              VARCHAR(64)  NOT NULL,   -- 老人姓名
    elder_age               INT,
    id_card                 VARCHAR(32),
    phone                   VARCHAR(32)  NOT NULL,   -- 家属联系方式
    address                 VARCHAR(256) NOT NULL,
    community               VARCHAR(128),
    floor                   INT,                     -- 居住楼层
    has_elevator            BOOLEAN      NOT NULL DEFAULT FALSE,
    living_alone            BOOLEAN      NOT NULL DEFAULT FALSE, -- 是否独居
    mobility                VARCHAR(32),             -- 行动能力: 独立/拐杖/轮椅/卧床
    fall_history            VARCHAR(256),            -- 跌倒史
    bathroom_status         VARCHAR(256),            -- 卫生间现状
    bedroom_status          VARCHAR(256),            -- 卧室现状
    house_ownership         VARCHAR(32),             -- 房屋权属: 自有/租赁/公房/子女
    expected_items          VARCHAR(512),            -- 期望改造项目
    status                  VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED',
    subsidy_eligible        BOOLEAN,                 -- 补贴资格核验结论
    verify_remark           VARCHAR(512),
    assessor_id             BIGINT REFERENCES app_user(id),
    applicant_user_id       BIGINT REFERENCES app_user(id),
    submitted_at            TIMESTAMP    NOT NULL DEFAULT now(),
    verified_at             TIMESTAMP,
    assigned_at             TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_application_status ON application(status);

-- 入户评估（评估师采集）
CREATE TABLE assessment (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL UNIQUE REFERENCES application(id),
    assessor_id             BIGINT REFERENCES app_user(id),
    threshold_height        NUMERIC(6,2),            -- 门槛高度 cm
    bathroom_width          NUMERIC(6,2),            -- 卫生间宽 cm
    bathroom_depth          NUMERIC(6,2),            -- 卫生间深 cm
    wall_material           VARCHAR(32),             -- 墙体材质: 实心砖墙/空心砖/轻质隔墙/瓷砖墙面
    night_lighting          VARCHAR(16),             -- 夜间照明: 充足/一般/昏暗
    bed_transfer_difficulty VARCHAR(512),            -- 床边起身困难点
    trial_actions           VARCHAR(512),            -- 老人试行动作
    fall_risk_level         VARCHAR(16),             -- 跌倒风险等级: 高/中/低
    summary                 VARCHAR(1024),
    assessed_at             TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

-- 改造方案条目（系统按评估结果生成 + 家属可增删）
CREATE TABLE plan_item (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL REFERENCES application(id),
    category                VARCHAR(32)  NOT NULL,   -- 扶手/防滑/坐便/床边护栏/感应夜灯/紧急呼叫/坡道
    name                    VARCHAR(128) NOT NULL,
    reason                  VARCHAR(512) NOT NULL,   -- 改造原因
    spec                    VARCHAR(256),
    unit                    VARCHAR(16),
    quantity                INT NOT NULL DEFAULT 1,
    unit_price              NUMERIC(10,2) NOT NULL,  -- 单价
    subsidy_cap             NUMERIC(10,2) NOT NULL,  -- 该项补贴上限
    construction_impact     VARCHAR(256),            -- 施工影响（噪音/工期/打孔等）
    status                  VARCHAR(16)  NOT NULL DEFAULT 'PROPOSED', -- PROPOSED/ACCEPTED/REMOVED/ADDED
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_plan_item_app ON plan_item(application_id);

-- 方案确认轮次（家属每次增删后，社区重新复核补贴）
CREATE TABLE plan_confirmation (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL REFERENCES application(id),
    round_no                INT NOT NULL DEFAULT 1,
    total_cost              NUMERIC(10,2) NOT NULL,
    subsidy_amount          NUMERIC(10,2) NOT NULL,
    self_pay                NUMERIC(10,2) NOT NULL,
    family_confirmed        BOOLEAN,
    family_confirmed_at     TIMESTAMP,
    family_signer           VARCHAR(64),
    community_approved      BOOLEAN,
    community_remark        VARCHAR(512),
    community_reviewed_at   TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_plan_conf_app ON plan_confirmation(application_id);

-- 施工排期
CREATE TABLE construction_schedule (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL UNIQUE REFERENCES application(id),
    team_id                 BIGINT REFERENCES app_user(id),
    scheduled_start         DATE,
    scheduled_end           DATE,
    building_access         VARCHAR(512),            -- 楼栋通行
    elevator_plan           VARCHAR(512),            -- 电梯使用安排
    material_arrival        VARCHAR(256),            -- 材料到货
    elder_schedule          VARCHAR(256),            -- 老人作息
    noise_restriction       VARCHAR(256),            -- 邻里噪音限制
    remark                  VARCHAR(512),
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);

-- 施工变更（现场异常 → 家属确认 → 社区复核，可多轮）
CREATE TABLE construction_change (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL REFERENCES application(id),
    reason_type             VARCHAR(32)  NOT NULL,   -- WALL_UNDRILLABLE/PIPE_BLOCK/HOSPITAL/FAMILY_CHANGE/MODEL_MISMATCH
    description             VARCHAR(512) NOT NULL,
    cost_delta              NUMERIC(10,2) NOT NULL DEFAULT 0,
    status                  VARCHAR(24)  NOT NULL DEFAULT 'SUBMITTED', -- SUBMITTED/FAMILY_CONFIRMED/COMMUNITY_APPROVED/REJECTED
    family_confirmed_at     TIMESTAMP,
    family_opinion          VARCHAR(512),
    community_remark        VARCHAR(512),
    community_reviewed_at   TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_change_app ON construction_change(application_id);

-- 竣工验收
CREATE TABLE completion (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL UNIQUE REFERENCES application(id),
    before_photos           VARCHAR(1024),           -- 改造前照片（URL/说明，逗号分隔）
    after_photos            VARCHAR(1024),           -- 改造后照片
    trial_record            VARCHAR(1024),           -- 老人试用记录
    family_signer           VARCHAR(64),             -- 家属签字
    signed_at               TIMESTAMP,
    materials_detail        VARCHAR(1024),           -- 施工材料明细
    cost_detail             VARCHAR(1024),           -- 费用明细
    total_cost              NUMERIC(10,2),
    completed_at            TIMESTAMP
);

-- 街道补贴审核
CREATE TABLE subsidy_review (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL UNIQUE REFERENCES application(id),
    reviewer_id             BIGINT REFERENCES app_user(id),
    approved_subsidy        NUMERIC(10,2),
    self_pay                NUMERIC(10,2),
    conclusion              VARCHAR(16),             -- APPROVED/REJECTED
    remark                  VARCHAR(512),
    reviewed_at             TIMESTAMP
);

-- 结算（审核结论回写施工队）
CREATE TABLE settlement (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL UNIQUE REFERENCES application(id),
    total_amount            NUMERIC(10,2),
    subsidy_amount          NUMERIC(10,2),
    family_pay_amount       NUMERIC(10,2),
    team_pay_amount         NUMERIC(10,2),           -- 应付施工队
    status                  VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/SETTLED
    settled_at              TIMESTAMP
);

-- 质保回访
CREATE TABLE warranty_visit (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL REFERENCES application(id),
    visit_time              TIMESTAMP,
    result                  VARCHAR(16),             -- SATISFIED/ISSUES
    content                 VARCHAR(1024),
    next_visit_date         DATE,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_warranty_app ON warranty_visit(application_id);

-- 流程时间线
CREATE TABLE workflow_log (
    id                      BIGSERIAL PRIMARY KEY,
    application_id          BIGINT NOT NULL REFERENCES application(id),
    action                  VARCHAR(64)  NOT NULL,
    from_status             VARCHAR(32),
    to_status               VARCHAR(32),
    operator_id             BIGINT REFERENCES app_user(id),
    operator_name           VARCHAR(64),
    note                    VARCHAR(512),
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_log_app ON workflow_log(application_id);
