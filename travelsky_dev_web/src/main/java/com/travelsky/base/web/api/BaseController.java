package com.travelsky.base.web.api;


import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

public interface BaseController<T, V, ID> {

    @GetMapping(value = "{id}")
    @Operation(summary = "根据ID获取")
    V getOne(@Parameter(name = "id", description = "ID", required = true, in = ParameterIn.PATH) @PathVariable("id") ID id);

    @GetMapping
    @Operation(summary = "根据条件获取分页数据")
    Page<V> findAll(DevQueryVO devQueryVO);

    @PostMapping
    @Operation(summary = "新增")
    @Parameter(name = "v", description = "实体对象", required = true)
    V save(@RequestBody V v);

    @PutMapping("{id}")
    @Operation(summary = "根据ID修改")
    V update(@Parameter(name = "id", description = "ID", required = true, in = ParameterIn.PATH) @PathVariable("id") ID id, @RequestBody V v);

    @DeleteMapping("{id}")
    @Operation(summary = "根据ID删除")
    void delete(@Parameter(name = "id", description = "ID", required = true, in = ParameterIn.PATH) @PathVariable("id") ID id);

    @PostMapping("batch")
    @Operation(summary = "批量新增")
    Collection<V> saveBatch(@RequestBody Collection<V> vs);

    @PutMapping("batch")
    @Operation(summary = "批量修改")
    Collection<V> updateBatch(@RequestBody Collection<V> vs);

    @DeleteMapping("batch")
    @Operation(summary = "根据ID批量删除")
    void deleteBatch(@RequestBody Collection<ID> ids);
}
