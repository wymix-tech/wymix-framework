static def generateCode(DatabaseTableFetcher fetcher, String tableName, String baseRoute,
                        boolean useRouteNameBeClassNamePrefix, String basePackage, List allTables, String path) {

    def columns
    def primaryKeys
    def tableComment

    def tmp = allTables.findAll { it.tableName.equalsIgnoreCase(tableName) }

    if (tmp.size() == 1) {
        tableComment = tmp[0].comment
    } else {
        tableComment = ""
    }

    try {
        columns = fetcher.getTableDetails(tableName)
        primaryKeys = fetcher.getPrimaryKeys(tableName)
    } catch (Exception e) {
        return
    }
    def idType = ''

    def classNamePrefix = Tools.caseFirstLetter(useRouteNameBeClassNamePrefix ? baseRoute : Tools.convertToCamelCase(tableName))

    def entity = """package ${basePackage}.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.travelsky.base.repository.mybatis.audit.domain.AbstractAuditEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "${tableName.toUpperCase()}")
public class ${classNamePrefix}Entity extends AbstractAuditEntity implements Serializable {
    """

    def vo = """package ${basePackage}.vo;

import com.travelsky.base.common.vo.AbstractAuditVO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "${tableComment == "" ? classNamePrefix : tableComment}")
public class ${classNamePrefix}VO extends AbstractAuditVO implements Serializable {
    """

    columns.each {
        if (it.columnName.toUpperCase() in ["CREATED_BY", "CREATED_TIME", "LAST_MODIFIED_BY", "LAST_MODIFIED_TIME"]) {
            return
        }
        if (it.columnName in primaryKeys) {
            entity += "\n    /* $it.comment */\n    @TableId(value = \"$it.columnName\", type = IdType.ASSIGN_ID)"
            idType = Tools.mapSqlTypeToJavaType(it.typeName, it.size, it.dataType)
        } else {
            entity += "\n    /* $it.comment */\n    @TableField(value = \"$it.columnName\")"
        }
        entity += "\n    private ${Tools.mapSqlTypeToJavaType(it.typeName, it.size, it.dataType)} ${Tools.convertToCamelCase(it.columnName.toLowerCase())};\n"
        vo += "\n    @ApiModelProperty(value = \"$it.comment\")\n    private ${Tools.mapSqlTypeToJavaType(it.typeName, it.size, it.dataType)} ${Tools.convertToCamelCase(it.columnName.toLowerCase())};\n"

    }
    entity += "\n}"
    vo += "\n}"

    def dao = """package  ${basePackage}.dao;

import com.travelsky.base.repository.mybatis.api.BaseRepository;
import  ${basePackage}.entity.${classNamePrefix}Entity;

public interface ${classNamePrefix}Dao extends BaseRepository<${classNamePrefix}Entity, ${idType}> {
    
}
    """

    def service = """package ${basePackage}.service;

import com.travelsky.base.core.api.BaseService;
import com.travelsky.base.repository.mybatis.api.AbstractBaseServiceImpl;
import ${basePackage}.dao.${classNamePrefix}Dao;
import ${basePackage}.entity.${classNamePrefix}Entity;
import org.springframework.stereotype.Service;

@Service
public class ${classNamePrefix}Service extends AbstractBaseServiceImpl<${classNamePrefix}Entity, ${idType}, ${classNamePrefix}Dao> implements BaseService<${classNamePrefix}Entity, ${idType}> {
    public ${classNamePrefix}Service(${classNamePrefix}Dao repository) {
        super(repository);
    }
    
}
    """

    def controller = """package ${basePackage}.web;

import com.travelsky.base.web.api.BaseController;
import com.travelsky.base.web.api.impl.AbstractBaseController;
import ${basePackage}.entity.${classNamePrefix}Entity;
import ${basePackage}.vo.${classNamePrefix}VO ;
import ${basePackage}.service.${classNamePrefix}Service;

import io.swagger.annotations.Api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${baseRoute}")
@Api(tags = "${tableComment == "" ? classNamePrefix : tableComment}管理")
public class ${classNamePrefix}Controller extends AbstractBaseController<${classNamePrefix}Entity,${classNamePrefix}VO,${idType}, ${classNamePrefix}Service> implements BaseController<${classNamePrefix}Entity,${classNamePrefix}VO,${idType}> {
    public ${classNamePrefix}Controller(${classNamePrefix}Service service) {
        super(service);
    }
}
    """

    def packagePath = basePackage.split("\\.").join(File.separator)
    def output = path + File.separator + packagePath + File.separator
    Tools.writeToFile("${classNamePrefix}Entity", entity, output + "entity")
    Tools.writeToFile("${classNamePrefix}VO", vo, output + "vo")
    Tools.writeToFile("${classNamePrefix}Dao", dao, output + "dao")
    Tools.writeToFile("${classNamePrefix}Service", service, output + "service")
    Tools.writeToFile("${classNamePrefix}Controller", controller, output + "web")
    println "entity, vo, dao, service, web 相关代码生成完毕。"
}


// 数据库连接信息通过环境变量或系统属性注入（避免硬编码凭证入库），缺失时快速失败
def config = [:]
config.url = System.getenv("GEN_DB_URL") ?: System.getProperty("gen.db.url")
config.user = System.getenv("GEN_DB_USER") ?: System.getProperty("gen.db.user")
config.password = System.getenv("GEN_DB_PASSWORD") ?: System.getProperty("gen.db.password")

if (!config.url || !config.user || !config.password) {
    println "[code-generator] Missing database config. Please set environment variables:"
    println "  GEN_DB_URL      e.g. jdbc:mysql://127.0.0.1:3306/your_db?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf8"
    println "  GEN_DB_USER     database username"
    println "  GEN_DB_PASSWORD database password"
    System.exit(1)
}

def fetcher

try {
    fetcher = new DatabaseTableFetcher(config.url, config.user, config.password)
} catch (Exception e) {
    println "[code-generator] Failed to connect database: ${e.message}"
    System.exit(1)
}

def allTables = fetcher.getAllTables()

// 包名
def basePackage = "com.swcares.ttimas"

// 是否使用路由名称作为类名前缀
def useRouteNameBeClassNamePrefix = true

def path = "C:\\Project\\TTIMAS\\src\\main\\java"

def scanner = new Scanner(System.in)


while (true) {
    println "\n请输入需要生成代码的表名:"
    def tableName = scanner.nextLine()
    println "\n请输入基本路由地址:"
    def route = scanner.nextLine()
    generateCode(fetcher, tableName, route, useRouteNameBeClassNamePrefix, basePackage, allTables, path)
}