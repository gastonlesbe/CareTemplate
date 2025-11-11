package com.gastonlesbegueris.caretemplate.ui;

import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;

public class LimitGuard {
    private final EventDao eventDao;
    private final SubjectDao subjectDao;
    private final String appType;

    public LimitGuard(EventDao eventDao, SubjectDao subjectDao, String appType) {
        this.eventDao = eventDao;
        this.subjectDao = subjectDao;
        this.appType = appType;
    }

    /** cuántos sujetos activos hay */
    public int subjectCount() {
        try { return subjectDao.countForApp(appType); } catch (Exception e) { return 0; }
    }

    /** cuántos eventos activos hay */
    public int eventCount() {
        try { return eventDao.countEventsForApp(appType); } catch (Exception e) { return 0; }
    }

    /** ¿supera el límite gratis de sujetos? */
    public boolean subjectsOverLimit(int maxFree) { return subjectCount() >= maxFree; }

    /** ¿supera el límite gratis de eventos? */
    public boolean eventsOverLimit(int maxFree) { return eventCount() >= maxFree; }
}
