CREATE DATABASE `mcp` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

-- mcp.t_chat_message definition

CREATE TABLE `t_chat_message` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id，自增',
  `session_id` varchar(100) NOT NULL DEFAULT '' COMMENT '会话id，一个会话id对应多条single_id',
  `single_id` varchar(100) NOT NULL DEFAULT '' COMMENT '单次对话id，例如，值可以为应用中一次请求的logId',
  `type` varchar(100) NOT NULL DEFAULT '' COMMENT '消息的类型，取值为ChatMessage的子类，包括SystemMessage、UserMessage、AiMessage、ToolExecutionResultMessage、CustomMessage等',
  `content` mediumtext COMMENT '消息的内容',
  `state` int(11) NOT NULL DEFAULT '0' COMMENT '是否有效，0为有效，1为无效，默认为0',
  `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `t_mtime_IDX` (`mtime`) USING BTREE,
  KEY `t_session_id_IDX` (`session_id`) USING BTREE,
  KEY `t_single_id_IDX` (`single_id`) USING BTREE,
  KEY `t_chat_message_type_IDX` (`type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2236 DEFAULT CHARSET=utf8mb4 COMMENT='会话消息表，记录每次会话的历史消息';


-- mcp.t_label definition

CREATE TABLE `t_label` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '标签id',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '标签类别，默认值为0，表示无分类',
  `name` varchar(100) NOT NULL DEFAULT '' COMMENT '标签名，可以表示大模型角色，或分类等',
  `description` mediumtext NOT NULL COMMENT '标签描述，例如，可以是systemprompt',
  `ext` varchar(5000) NOT NULL DEFAULT '{}' COMMENT '扩展字段，为一个jsonobject',
  `parent_id` bigint(20) NOT NULL DEFAULT '-1' COMMENT '父级标签id，用于实现标签的层级，默认为-1，表示无父标签',
  `state` int(11) NOT NULL DEFAULT '0' COMMENT '是否有效，0为有效，1为无效',
  `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `t_label_un_name` (`name`),
  KEY `t_param_type_IDX` (`type`) USING BTREE,
  KEY `t_param_name_IDX` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COMMENT='标签表';


-- mcp.t_label_tool definition

CREATE TABLE `t_label_tool` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `label_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '标签id， 对应t_label表中的主键id',
  `tool_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '方法id，对应t_tool表中的主键id',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '类型',
  `state` int(11) NOT NULL DEFAULT '0' COMMENT '是否有效，0为有效，1为无效',
  `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `t_label_method_label_id_IDX` (`label_id`) USING BTREE,
  KEY `t_label_method_method_id_IDX` (`tool_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COMMENT='标签方法关系表';


-- mcp.t_prompt definition

CREATE TABLE `t_prompt` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '方法id',
  `model` varchar(100) NOT NULL DEFAULT '' COMMENT '模块名（可作为prompt分类，也可看成Class名）',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '类型，默认值为0，表示text类型，1表示resource类型，2表示image类型',
  `name` varchar(100) NOT NULL COMMENT 'prompt名，必需符合程序名的规范，不能为中文。',
  `description` mediumtext NOT NULL COMMENT 'prompt描述',
  `param_schema` mediumtext NOT NULL COMMENT 'prompt参数的json schema。 格式为JSON数组，默认值为[]，包含name、description、require属性。示例：\n[{\n	"name": "code",\n	"description": "要解释的代码",\n	"required": true\n}, {\n	"name": "language",\n	"description": "程序语言",\n	"required": false\n}]',
  `prompt_template` mediumtext COMMENT 'prompt模版内容，其中包含${var}等变量。示例：\n解释${language!''java''}代码是如何工作的：\\n\\n\n${code}',
  `prompt_config` varchar(5000) NOT NULL DEFAULT '{}' COMMENT 'prompt的配置，例如指定role值为user或assistant：\n{\n	"role": "user"\n}',
  `state` int(11) NOT NULL DEFAULT '0' COMMENT '是否有效，0为有效，1为无效',
  `version` varchar(20) NOT NULL DEFAULT '' COMMENT 'prompt版本',
  `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_t_prompt_un` (`name`),
  KEY `t_method_name_IDX` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='prompt表';


-- mcp.t_server definition

CREATE TABLE `t_server` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '服务id',
  `type` varchar(50) NOT NULL DEFAULT 'http' COMMENT '服务类型，可选值local本地方法，http方法',
  `name` varchar(100) NOT NULL COMMENT '服务名称',
  `description` varchar(2000) NOT NULL COMMENT '服务描述',
  `datasource_desc` varchar(5000) NOT NULL DEFAULT '{}' COMMENT '数据源的dataSourceDesc配置',
  `datasource_config` varchar(5000) NOT NULL DEFAULT '{}' COMMENT '数据源的dataSourceConfig配置',
  PRIMARY KEY (`id`),
  KEY `t_server_type_IDX` (`type`) USING BTREE,
  KEY `t_server_name_IDX` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='服务表';


-- mcp.t_tool definition

CREATE TABLE `t_tool` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '方法id',
  `model` varchar(100) NOT NULL DEFAULT '' COMMENT '模块名（可作为方法分类，也可视为Class名）',
  `type` int(11) NOT NULL DEFAULT '0' COMMENT '类型，默认值为0，表示无类型',
  `name` varchar(100) NOT NULL COMMENT '方法名，必需符合程序名的规范，不能为中文。',
  `description` mediumtext NOT NULL COMMENT '方法描述',
  `param_schema` mediumtext NOT NULL COMMENT '参数json schema',
  `return_class` mediumtext NOT NULL COMMENT '返回值的类型。是一个java类型，具体参照https://docs.langchain4j.dev/tutorials/ai-services定义Agent返回值类型',
  `return_script` mediumtext NOT NULL COMMENT '处理返回值的脚本',
  `datasource_desc` varchar(5000) NOT NULL DEFAULT '{}' COMMENT '数据源的dataSourceDesc配置',
  `datasource_config` varchar(5000) NOT NULL DEFAULT '{}' COMMENT '数据源的dataSourceDesc配置',
  `state` int(11) NOT NULL DEFAULT '0' COMMENT '是否有效，0为有效，1为无效',
  `version` varchar(20) NOT NULL DEFAULT '' COMMENT '方法版本',
  `mtime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_t_tool_un` (`name`),
  KEY `t_method_name_IDX` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COMMENT='方法表';