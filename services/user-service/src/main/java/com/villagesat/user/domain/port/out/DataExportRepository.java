package com.villagesat.user.domain.port.out;

import com.villagesat.user.domain.model.DataExportRequest;

public interface DataExportRepository {

    DataExportRequest save(DataExportRequest request);
}
