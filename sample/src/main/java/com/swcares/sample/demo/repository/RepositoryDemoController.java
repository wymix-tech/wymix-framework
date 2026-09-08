package com.swcares.sample.demo.repository;

import com.swcares.sample.entity.SampleUserEntity;
import com.swcares.sample.service.SampleUserService;
import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.common.query.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * travelsky_dev_mybatis_repository 示例：{@code BaseService} 全量 API + 查询 DSL。
 *
 * <p>{@code SampleUserService} 继承 {@code AbstractBaseServiceImpl} 即获得下列全部能力，
 * 本控制器逐个暴露为端点，便于对照验证：
 * <ul>
 *     <li>写：{@code save / update / delete / batchSave / batchUpdate / deleteAll / deleteAllById}</li>
 *     <li>读：{@code getOne / findAllById / findAll() / findAll(DevQueryVO)}</li>
 *     <li>DSL：{@code filter} 过滤 + {@code sorts} 排序 + 分页（见 {@code /operators}）</li>
 * </ul>
 */
@RestController
@RequestMapping("demo/repository")
@Tag(name = "mybatis_repository 模块示例")
public class RepositoryDemoController {

    private final SampleUserService sampleUserService;

    public RepositoryDemoController(SampleUserService sampleUserService) {
        this.sampleUserService = sampleUserService;
    }

    // ---------- 读 ----------

    @GetMapping("{id}")
    @Operation(summary = "getOne：按主键查询")
    public SampleUserEntity getOne(@PathVariable Long id) {
        return sampleUserService.getOne(id);
    }

    @GetMapping
    @Operation(summary = "findAll()：全量查询")
    public List<SampleUserEntity> findAll() {
        return sampleUserService.findAll();
    }

    @GetMapping("ids")
    @Operation(summary = "findAllById：按主键集合查询")
    public List<SampleUserEntity> findAllById(@RequestParam List<Long> ids) {
        return sampleUserService.findAllById(ids);
    }

    @GetMapping("page")
    @Operation(summary = "findAll(DevQueryVO)：过滤 + 排序 + 分页")
    public Page<SampleUserEntity> page(@RequestParam(required = false) String username,
                                       @RequestParam(defaultValue = "1") Long page,
                                       @RequestParam(defaultValue = "10") Long limit) {
        DevQueryVO queryVO = new DevQueryVO();
        queryVO.setPage(page);
        queryVO.setLimit(limit);
        if (username != null && !username.isBlank()) {
            // [[username,CONTAINS,xxx]]  —— 字段名按驼峰转下划线映射列
            queryVO.addFilters(Triple.of("username", OperateSymbol.CONTAINS, username));
        }
        // 排序：sorts 为 JSON 数组，columns 为字段名集合，direction 为 ASC/DESC
        queryVO.setSorts("[{\"columns\":[\"id\"],\"direction\":\"DESC\"}]");
        return sampleUserService.findAll(queryVO);
    }

    /**
     * DevQueryVO 原生 DSL 入口：直接把 filter / sorts / page / limit 透传给 {@code BaseService#findAll}，
     * 便于逐个验证操作符与排序，无需为每个组合各写一个端点。
     *
     * <p>filter 语法（大小写不敏感）：
     * <pre>
     * 单条件      [[username,=,alpha]]
     * 多条件 AND  [[username,=,alpha],[password,=,p1]]
     * 或          [[username,=,alpha],or,[[username,=,beta]]]
     * 与或嵌套    [[username,=,alpha],or,[[username,=,beta],and,[[password,=,p1]]]]
     * IN          [[username,in,[alpha,beta]]]
     * </pre>
     * sorts 语法：[{"columns":["username"],"direction":"DESC"}]
     */
    @GetMapping("dsl")
    @Operation(summary = "DevQueryVO 原生 DSL：直接传 filter / sorts / page / limit")
    public Page<SampleUserEntity> dsl(@RequestParam(required = false) String filter,
                                      @RequestParam(required = false) String sorts,
                                      @RequestParam(defaultValue = "1") Long page,
                                      @RequestParam(defaultValue = "10") Long limit) {
        DevQueryVO queryVO = new DevQueryVO();
        queryVO.setFilter(filter);
        queryVO.setSorts(sorts);
        queryVO.setPage(page);
        queryVO.setLimit(limit);
        return sampleUserService.findAll(queryVO);
    }

    @GetMapping("permission")
    @Operation(summary = "@DataPermission：方法级数据权限（SQL 追加过滤条件）")
    public List<SampleUserEntity> permission() {
        // SampleUserService#findAllWithDataPermission 上标注了 @DataPermission(injectors = TestSqlInject.class)
        return sampleUserService.findAllWithDataPermission();
    }

    // ---------- 写 ----------

    @PostMapping
    @Operation(summary = "save：新增单条（审计字段由 AbstractAuditEntity + UserContextHolder 自动填充）")
    public SampleUserEntity save(@RequestBody SampleUserEntity entity) {
        return sampleUserService.save(entity);
    }

    @PutMapping("{id}")
    @Operation(summary = "update：按主键更新")
    public SampleUserEntity update(@PathVariable Long id, @RequestBody SampleUserEntity entity) {
        return sampleUserService.update(id, entity);
    }

    @DeleteMapping("{id}")
    @Operation(summary = "delete：按主键删除")
    public void delete(@PathVariable Long id) {
        sampleUserService.delete(id);
    }

    @PostMapping("batch")
    @Operation(summary = "batchSave：批量新增")
    public Collection<SampleUserEntity> batchSave(@RequestBody List<SampleUserEntity> entities) {
        return sampleUserService.batchSave(entities);
    }

    @PutMapping("batch")
    @Operation(summary = "batchUpdate：批量更新")
    public Collection<SampleUserEntity> batchUpdate(@RequestBody List<SampleUserEntity> entities) {
        return sampleUserService.batchUpdate(entities);
    }

    @DeleteMapping("batch")
    @Operation(summary = "deleteAllById：按主键集合批量删除")
    public void deleteAllById(@RequestParam List<Long> ids) {
        sampleUserService.deleteAllById(ids);
    }

    @DeleteMapping("entities")
    @Operation(summary = "deleteAll：按实体集合批量删除")
    public void deleteAll(@RequestBody List<SampleUserEntity> entities) {
        sampleUserService.deleteAll(entities);
    }

    // ---------- DSL 速查 ----------

    @GetMapping("operators")
    @Operation(summary = "DevQueryVO 支持的全部过滤操作符")
    public Map<String, List<String>> operators() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (OperateSymbol symbol : OperateSymbol.values()) {
            result.put(symbol.name(), Arrays.asList(symbol.getSymbol()));
        }
        return result;
    }

    @GetMapping("sorts")
    @Operation(summary = "SortVO 排序字段的 JSON 结构示例")
    public Map<String, Object> sorts() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sorts", "[{\"columns\":[\"id\"],\"direction\":\"DESC\"}]");
        result.put("columns", "字段名集合（驼峰，框架按列映射规则转换）");
        result.put("direction", "ASC | DESC");
        result.put("filterExample", Arrays.asList(OperateSymbol.values()).stream()
                .limit(3)
                .map(s -> s.name() + "->" + s.getSymbol()[0])
                .collect(Collectors.toList()));
        return result;
    }
}
