package dev.abhay.gitbrain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncSummary {
    private int addedCount;
    private int updatedCount;
    private int deletedCount;
}