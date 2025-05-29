package com.DDT.javaWeb.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForumCategoryVO implements Serializable {
    private Long id;
    private String name;
    private Integer sortOrder;
}
