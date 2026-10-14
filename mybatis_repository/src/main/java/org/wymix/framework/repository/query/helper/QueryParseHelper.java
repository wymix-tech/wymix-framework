package org.wymix.framework.repository.query.helper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.OperateSymbol;
import org.wymix.framework.common.query.SortVO;
import org.wymix.framework.repository.query.FilterParser;
import org.wymix.framework.repository.query.FilterStringParser;
import org.wymix.framework.repository.query.ast.ConditionNode;
import org.wymix.framework.repository.query.ast.FilterNode;
import org.wymix.framework.repository.query.ast.GroupNode;
import org.wymix.framework.repository.query.ast.LogicalNode;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.utils.json.JsonUtils;

import java.util.Arrays;
import java.util.List;

/**
 * MyBatis 查询构造器：将统一 filter DSL 的 {@link FilterNode} AST 翻译为 {@link QueryWrapper}。
 * <p>filter 词法/语法解析已上提至 repository（{@link FilterParser} / {@link FilterStringParser}），
 * 本类仅负责 AST -> QueryWrapper 的翻译，新增操作符只需在 {@link #addCondition} 增加分支。
 */
public class QueryParseHelper {

    /**
     * 列名风格策略：根据 {@code COLUMN_NAME_RULE} 系统属性决定是否做驼峰转下划线。
     * <p>缓存只读快照，配置变更时通过 {@link #refresh()} 重建，避免静态可变状态带来的可见性问题。
     */
    private static volatile ColumnNameStyleStrategy strategy = ColumnNameStyleStrategy.fromSystemProperty();

    /**
     * 兼容旧版本预留的入口：刷新列名风格策略（重新读取系统属性）。
     * <p>仅当运行期通过 {@link System#setProperty(String, String)} 修改 {@code COLUMN_NAME_RULE} 后调用。
     */
    public static void refresh() {
        strategy = ColumnNameStyleStrategy.fromSystemProperty();
    }

    static void setStrategy(ColumnNameStyleStrategy newStrategy) {
        strategy = newStrategy == null ? ColumnNameStyleStrategy.fromSystemProperty() : newStrategy;
    }

    static boolean isCamelToUnderline() {
        return strategy.camelToUnderline();
    }

    public static <T> QueryWrapper<T> parseToQueryWrapper(QueryWrapper<T> wrapper, DevQueryVO query, Class<T> clazz) {
        if (StringUtils.isNotEmpty(query.getFilter())) {
            parse(wrapper, query.getFilter(), clazz);
        }
        String sort = query.getSorts();
        if (StringUtils.isNotEmpty(sort)) {
            List<SortVO> sortVOs = JsonUtils.jsonToEntity(List.class, SortVO.class, sort);
            if (sortVOs == null) throw new IllegalArgumentException("Invalid sorts JSON");
            for (SortVO sortVO : sortVOs) {
                if (sortVO == null || sortVO.getDirection() == null || sortVO.getColumns() == null || sortVO.getColumns().length == 0) {
                    throw new IllegalArgumentException("Sort direction and columns are required");
                }
                if (sortVO.getDirection() == SortVO.Type.ASC) {
                    wrapper.orderByAsc(Arrays.stream(sortVO.getColumns()).map(e->toColumnName(e,clazz)).toList());
                } else {
                    wrapper.orderByDesc(Arrays.stream(sortVO.getColumns()).map(e->toColumnName(e,clazz)).toList());
                }
            }
        }
        return wrapper;
    }

    private static <T> void parse(QueryWrapper<T> wrapper, String condition, Class<T> clazz) {
        try {
            FilterNode ast = FilterParser.parse(condition);
            if (ast != null) {
                translate(ast, wrapper, clazz);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid query filter", e);
        }
    }

    /**
     * AST -> QueryWrapper 翻译：按节点类型分派。
     */
    private static <T> void translate(FilterNode node, QueryWrapper<T> wrapper, Class<T> clazz) {
        if (node instanceof ConditionNode condition) {
            addCondition(wrapper, condition, clazz);
        } else if (node instanceof LogicalNode logical) {
            if (logical.type() == LogicalNode.Type.AND) {
                wrapper.and(w -> translate(logical.left(), w, clazz));
                // AND 语义下右侧也并入 and 分支，保持与既有 and(...) 嵌套等价
                wrapper.and(w -> translate(logical.right(), w, clazz));
            } else {
                wrapper.or(w -> translate(logical.left(), w, clazz));
                wrapper.or(w -> translate(logical.right(), w, clazz));
            }
        } else if (node instanceof GroupNode group) {
            wrapper.and(w -> {
                for (FilterNode child : group.children()) {
                    translate(child, w, clazz);
                }
            });
        } else {
            throw new IllegalArgumentException("Unsupported filter node: " + node);
        }
    }

    private static <T> void addCondition(QueryWrapper<T> wrapper, ConditionNode condition, Class<T> clazz) {
        String field = condition.field();
        OperateSymbol operator = condition.operator();
        List<Object> values = condition.values();

        switch (operator) {
            case EQ:
                wrapper.eq(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case NE:
                wrapper.ne(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case GE:
                wrapper.ge(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LE:
                wrapper.le(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case GT:
                wrapper.gt(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LT:
                wrapper.lt(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case CONTAINS:
                wrapper.like(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LIKE_LEFT:
                wrapper.likeLeft(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LIKE_RIGHT:
                wrapper.likeRight(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case BETWEEN:
                if (values.size() == 2) {
                    wrapper.between(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()), FieldHelper.translate(clazz, field, values.get(1).toString()));
                }
                break;
            case IN:
                wrapper.in(toColumnName(field, clazz), values.stream().map(v -> FieldHelper.translate(clazz, field, v.toString())).toList());
                break;
            case NOT_IN:
                wrapper.notIn(toColumnName(field, clazz), values.stream().map(v -> FieldHelper.translate(clazz, field, v.toString())).toList());
                break;
            case NULL:
                wrapper.isNull(toColumnName(field, clazz));
                break;
            case NOT_NULL:
                wrapper.isNotNull(toColumnName(field, clazz));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private static final <T> String toColumnName(String field, Class<T> clazz) {
        return FieldHelper.addConditionAlias(clazz, field, formatToColumnName(field));
    }
    public static String formatToColumnName(String field) {
        String tableAlias = null;
        if (field.contains(".")){
            String[] temp = field.split("\\.");
            tableAlias=temp[0];
            field = temp[1];
        }
        return (StringUtils.isNotEmpty(tableAlias)?tableAlias+".":"")+(isCamelToUnderline() ? StringUtils.camelToUnderline(field) : field);
    }

    /**
     * 列名风格策略（任务4：原静态可变状态 CAMEL_TO_UNDERLINE 改为不可变策略对象）。
     */
    public static final class ColumnNameStyleStrategy {

        private final boolean camelToUnderline;

        private ColumnNameStyleStrategy(boolean camelToUnderline) {
            this.camelToUnderline = camelToUnderline;
        }

        public boolean camelToUnderline() {
            return camelToUnderline;
        }

        public static ColumnNameStyleStrategy camelToUnderline(boolean camelToUnderline) {
            return new ColumnNameStyleStrategy(camelToUnderline);
        }

        public static ColumnNameStyleStrategy fromSystemProperty() {
            return new ColumnNameStyleStrategy("CAMEL_TO_UNDERLINE".equals(System.getProperty("COLUMN_NAME_RULE", "CAMEL_TO_UNDERLINE")));
        }
    }
}
