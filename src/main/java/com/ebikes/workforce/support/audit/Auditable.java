package com.ebikes.workforce.support.audit;

import java.util.Map;

public interface Auditable {
  Map<String, String> toAuditMetadata();
}
