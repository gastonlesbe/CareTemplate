package com.gastonlesbegueris.caretemplate.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.gastonlesbegueris.caretemplate.data.local.AppDb;
import com.gastonlesbegueris.caretemplate.data.local.EventDao;
import com.gastonlesbegueris.caretemplate.data.local.EventEntity;
import com.gastonlesbegueris.caretemplate.data.local.SubjectDao;
import com.gastonlesbegueris.caretemplate.data.local.SubjectEntity;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Helper class to populate demo data for all flavors.
 * Creates 1 subject per flavor with sample events.
 */
public class DemoDataHelper {
    private static final String TAG = "DemoDataHelper";
    
    /**
     * Muestra un Toast de forma segura, evitando crashes en contextos de test.
     */
    private static void safeToast(Context context, String message, int duration) {
        if (context == null) {
            Log.d(TAG, "Toast not shown: context is null");
            return;
        }
        try {
            String packageName = context.getPackageName();
            // No mostrar Toast si estamos en un contexto de test
            if (packageName != null && (packageName.contains("test") || packageName.contains("android.test"))) {
                Log.d(TAG, "Toast not shown (test context): " + message);
                return;
            }
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                try {
                    Toast.makeText(context, message, duration).show();
                } catch (Exception e) {
                    Log.d(TAG, "Toast not shown (error): " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "Toast not shown (exception): " + e.getMessage());
        }
    }
    
    /**
     * Populates demo data for a specific app type (flavor).
     * @param context The context
     * @param appType The app type: "family", "pets", "cars", or "house"
     * @param callback Callback to execute when done (can be null)
     */
    public static void populateDemoData(Context context, String appType, Runnable callback) {
        new Thread(() -> {
            try {
                AppDb db = AppDb.get(context);
                SubjectDao subjectDao = db.subjectDao();
                EventDao eventDao = db.eventDao();
                
                // Get or create userId
                UserManager userManager = new UserManager(context);
                String userId = userManager.getUserIdSync();
                if (userId == null) {
                    // Generate a demo userId
                    userId = "demo_" + UUID.randomUUID().toString();
                    userManager.setUserId(userId);
                }
                
                // Check if demo data already exists for this flavor
                int existingCount = subjectDao.countForApp(appType);
                if (existingCount > 0) {
                    safeToast(context, "Demo data already exists for " + appType, Toast.LENGTH_SHORT);
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(callback);
                    }
                    return;
                }
                
                // Create subject based on flavor
                SubjectEntity subject = createDemoSubject(userId, appType);
                subjectDao.insert(subject);
                
                // Create demo events
                createDemoEvents(eventDao, userId, appType, subject.id);
                
                // Create demo expenses (6 months x 4 expenses = 24 expenses)
                createDemoExpenses(eventDao, userId, appType, subject.id);
                
                safeToast(context, "Demo data created for " + appType + "!", Toast.LENGTH_SHORT);
                Log.d(TAG, "Demo data populated for " + appType);
                
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error populating demo data", e);
                e.printStackTrace();
                safeToast(context, "Error creating demo data: " + e.getMessage(), Toast.LENGTH_LONG);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
            }
        }).start();
    }
    
    /**
     * Creates only subject and basic events (without expenses).
     * Useful for creating demo data in two steps.
     * @param context The context
     * @param appType The app type: "family", "pets", "cars", or "house"
     * @param callback Callback to execute when done (can be null)
     */
    public static void populateSubjectAndEvents(Context context, String appType, Runnable callback) {
        new Thread(() -> {
            try {
                // Asegurarse de usar ApplicationContext para la misma instancia de BD
                Context appContext = context.getApplicationContext();
                Log.d(TAG, "populateSubjectAndEvents - Package: " + appContext.getPackageName());
                
                AppDb db = AppDb.get(appContext);
                SubjectDao subjectDao = db.subjectDao();
                EventDao eventDao = db.eventDao();
                
                // Get or create userId
                UserManager userManager = new UserManager(appContext);
                String userId = userManager.getUserIdSync();
                if (userId == null) {
                    userId = "demo_" + UUID.randomUUID().toString();
                    userManager.setUserId(userId);
                }
                Log.d(TAG, "UserId: " + userId);
                
                // Check if subject already exists
                int existingCount = subjectDao.countForApp(appType);
                Log.d(TAG, "Existing subjects count: " + existingCount);
                if (existingCount > 0) {
                    Log.d(TAG, "Subject already exists, skipping creation");
                    safeToast(context, "Subject already exists for " + appType, Toast.LENGTH_SHORT);
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(callback);
                    }
                    return;
                }
                
                // Create subject based on flavor
                SubjectEntity subject = createDemoSubject(userId, appType);
                Log.d(TAG, "Creating subject: " + subject.name + " (id: " + subject.id + ")");
                subjectDao.insert(subject);
                Log.d(TAG, "Subject inserted successfully");
                
                // Verificar que se insertó correctamente
                int verifyCount = subjectDao.countForApp(appType);
                Log.d(TAG, "Verification: Subject count after insert = " + verifyCount);
                
                // Create demo events (only basic events, no expenses)
                Log.d(TAG, "Creating demo events...");
                createDemoEvents(eventDao, userId, appType, subject.id);
                Log.d(TAG, "Demo events created successfully");
                
                // Verificar que se crearon los eventos
                int eventCount = eventDao.countEventsForApp(appType);
                Log.d(TAG, "Verification: Event count after insert = " + eventCount);
                
                safeToast(context, "Subject and events created for " + appType + "!", Toast.LENGTH_SHORT);
                Log.d(TAG, "Subject and events populated for " + appType);
                
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error populating subject and events", e);
                e.printStackTrace();
                safeToast(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG);
                // NO re-lanzar la excepción aquí para evitar crashes que desinstalen la app
                // El test puede verificar el resultado de otra manera
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
            }
        }).start();
    }
    
    /**
     * Creates only expenses (6 months x 4 expenses = 24 expenses).
     * Requires that a subject already exists for the appType.
     * @param context The context
     * @param appType The app type: "family", "pets", "cars", or "house"
     * @param callback Callback to execute when done (can be null)
     */
    public static void populateExpenses(Context context, String appType, Runnable callback) {
        new Thread(() -> {
            try {
                AppDb db = AppDb.get(context);
                SubjectDao subjectDao = db.subjectDao();
                EventDao eventDao = db.eventDao();
                
                // Get or create userId
                UserManager userManager = new UserManager(context);
                String userId = userManager.getUserIdSync();
                if (userId == null) {
                    userId = "demo_" + UUID.randomUUID().toString();
                    userManager.setUserId(userId);
                }
                
                // Get first subject for this appType
                List<SubjectEntity> subjects = subjectDao.listActiveNow(appType);
                if (subjects == null || subjects.isEmpty()) {
                    safeToast(context, "No subject found for " + appType + ". Create subject first!", Toast.LENGTH_LONG);
                    if (callback != null) {
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(callback);
                    }
                    return;
                }
                
                String subjectId = subjects.get(0).id;
                
                // Create demo expenses (6 months x 4 expenses = 24 expenses)
                createDemoExpenses(eventDao, userId, appType, subjectId);
                
                safeToast(context, "Expenses created for " + appType + "!", Toast.LENGTH_SHORT);
                Log.d(TAG, "Expenses populated for " + appType);
                
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error populating expenses", e);
                e.printStackTrace();
                safeToast(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
            }
        }).start();
    }
    
    /**
     * Populates demo data for all flavors at once.
     */
    public static void populateAllFlavors(Context context, Runnable callback) {
        new Thread(() -> {
            try {
                AppDb db = AppDb.get(context);
                SubjectDao subjectDao = db.subjectDao();
                EventDao eventDao = db.eventDao();
                
                // Get or create userId
                UserManager userManager = new UserManager(context);
                String userId = userManager.getUserIdSync();
                if (userId == null) {
                    userId = "demo_" + UUID.randomUUID().toString();
                    userManager.setUserId(userId);
                }
                
                String[] flavors = {"family", "pets", "cars", "house"};
                int created = 0;
                
                for (String appType : flavors) {
                    // Only create if doesn't exist
                    if (subjectDao.countForApp(appType) == 0) {
                        SubjectEntity subject = createDemoSubject(userId, appType);
                        subjectDao.insert(subject);
                        createDemoEvents(eventDao, userId, appType, subject.id);
                        createDemoExpenses(eventDao, userId, appType, subject.id);
                        created++;
                    }
                }
                
                final int finalCreated = created;
                safeToast(context, "Created demo data for " + finalCreated + " flavor(s)", Toast.LENGTH_SHORT);
                Log.d(TAG, "Demo data populated for " + finalCreated + " flavors");
                
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error populating demo data for all flavors", e);
                e.printStackTrace();
                safeToast(context, "Error creating demo data: " + e.getMessage(), Toast.LENGTH_LONG);
                if (callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(callback);
                }
            }
        }).start();
    }
    
    private static SubjectEntity createDemoSubject(String userId, String appType) {
        SubjectEntity subject = new SubjectEntity();
        subject.id = userId + "_" + System.currentTimeMillis() + "_" + appType;
        subject.uid = userId;
        subject.appType = appType;
        subject.updatedAt = System.currentTimeMillis();
        subject.deleted = 0;
        subject.dirty = 1;
        subject.colorHex = "#03DAC5";
        subject.notes = "";
        
        Calendar cal = Calendar.getInstance();
        
        switch (appType) {
            case "family":
                subject.name = "John Doe";
                subject.iconKey = "man";
                // Birth date: 30 years ago
                cal.add(Calendar.YEAR, -30);
                subject.birthDate = cal.getTimeInMillis();
                subject.currentMeasure = 75.5; // weight in kg
                break;
                
            case "pets":
                subject.name = "Max";
                subject.iconKey = "dog";
                // Birth date: 2 years ago
                cal.add(Calendar.YEAR, -2);
                subject.birthDate = cal.getTimeInMillis();
                subject.currentMeasure = 15.0; // weight in kg
                break;
                
            case "cars":
                subject.name = "Toyota Corolla";
                subject.iconKey = "car";
                subject.birthDate = null; // Cars don't have birth dates
                subject.currentMeasure = 45000.0; // kilometers
                break;
                
            case "house":
                subject.name = "My Home";
                subject.iconKey = "house";
                subject.birthDate = null; // Houses don't have birth dates
                subject.currentMeasure = null; // Houses might not have a measure
                break;
                
            default:
                subject.name = "Demo Subject";
                subject.iconKey = "user";
                subject.birthDate = null;
                subject.currentMeasure = null;
        }
        
        return subject;
    }
    
    private static void createDemoEvents(EventDao eventDao, String userId, String appType, String subjectId) {
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        
        // Create a mix of past and future events
        EventEntity[] events = new EventEntity[5];
        
        // Event 1: Past event (completed) - 1 week ago
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_MONTH, -7);
        events[0] = createEvent(userId, appType, subjectId, 
            getEventTitle(appType, 1), 
            cal.getTimeInMillis(), 
            true, // realized
            getEventCost(appType, 1));
        
        // Event 2: Past event (completed) - 3 days ago
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_MONTH, -3);
        events[1] = createEvent(userId, appType, subjectId, 
            getEventTitle(appType, 2), 
            cal.getTimeInMillis(), 
            true, // realized
            getEventCost(appType, 2));
        
        // Event 3: Future event - tomorrow
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 10);
        cal.set(Calendar.MINUTE, 0);
        events[2] = createEvent(userId, appType, subjectId, 
            getEventTitle(appType, 3), 
            cal.getTimeInMillis(), 
            false, // not realized
            getEventCost(appType, 3));
        events[2].notificationMinutesBefore = 60; // 1 hour before
        
        // Event 4: Future event - 1 week from now
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_MONTH, 7);
        cal.set(Calendar.HOUR_OF_DAY, 14);
        cal.set(Calendar.MINUTE, 30);
        events[3] = createEvent(userId, appType, subjectId, 
            getEventTitle(appType, 4), 
            cal.getTimeInMillis(), 
            false, // not realized
            getEventCost(appType, 4));
        events[3].notificationMinutesBefore = 1440; // 1 day before
        
        // Event 5: Future event - 1 month from now (with repetition)
        cal.setTimeInMillis(now);
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        events[4] = createEvent(userId, appType, subjectId, 
            getEventTitle(appType, 5), 
            cal.getTimeInMillis(), 
            false, // not realized
            getEventCost(appType, 5));
        events[4].repeatType = "monthly";
        events[4].repeatInterval = 1;
        events[4].repeatCount = 12; // Repeat for 12 months
        events[4].notificationMinutesBefore = 1440; // 1 day before
        
        // Insert all events
        for (EventEntity event : events) {
            eventDao.insert(event);
        }
    }
    
    private static EventEntity createEvent(String userId, String appType, String subjectId, 
                                          String title, long dueAt, boolean realized, Double cost) {
        EventEntity event = new EventEntity();
        event.id = UUID.randomUUID().toString();
        event.uid = userId;
        event.appType = appType;
        event.subjectId = subjectId;
        event.title = title;
        event.note = "";
        event.dueAt = dueAt;
        event.cost = cost;
        event.realized = realized ? 1 : 0;
        event.realizedAt = realized ? dueAt : null;
        event.updatedAt = System.currentTimeMillis();
        event.deleted = 0;
        event.dirty = 1;
        event.repeatType = null;
        event.repeatInterval = null;
        event.repeatEndDate = null;
        event.repeatCount = null;
        event.originalEventId = null;
        event.notificationMinutesBefore = null;
        
        // For cars, add kilometers at event
        if ("cars".equals(appType) && realized) {
            event.kilometersAtEvent = 45000.0 + (System.currentTimeMillis() - dueAt) / (1000L * 60 * 60 * 24) * 50; // Approximate km
        } else {
            event.kilometersAtEvent = null;
        }
        
        return event;
    }
    
    private static String getEventTitle(String appType, int eventNum) {
        switch (appType) {
            case "family":
                switch (eventNum) {
                    case 1: return "Medical Checkup";
                    case 2: return "Dentist Appointment";
                    case 3: return "Vaccination";
                    case 4: return "Annual Physical";
                    case 5: return "Health Insurance Renewal";
                    default: return "Family Event";
                }
            case "pets":
                switch (eventNum) {
                    case 1: return "Vet Visit";
                    case 2: return "Vaccination";
                    case 3: return "Grooming";
                    case 4: return "Annual Checkup";
                    case 5: return "Monthly Medication";
                    default: return "Pet Care Event";
                }
            case "cars":
                switch (eventNum) {
                    case 1: return "Oil Change";
                    case 2: return "Tire Rotation";
                    case 3: return "Car Wash";
                    case 4: return "Annual Inspection";
                    case 5: return "Monthly Service";
                    default: return "Car Maintenance";
                }
            case "house":
                switch (eventNum) {
                    case 1: return "Plumbing Repair";
                    case 2: return "Electrical Check";
                    case 3: return "HVAC Maintenance";
                    case 4: return "Annual Inspection";
                    case 5: return "Monthly Utilities";
                    default: return "House Maintenance";
                }
            default:
                return "Demo Event " + eventNum;
        }
    }
    
    private static Double getEventCost(String appType, int eventNum) {
        // Some events have costs, some don't
        if (eventNum == 1 || eventNum == 4) {
            switch (appType) {
                case "family": return 150.0;
                case "pets": return 80.0;
                case "cars": return 120.0;
                case "house": return 200.0;
                default: return 100.0;
            }
        }
        return null; // No cost for other events
    }
    
    /**
     * Creates demo expenses: 6 months x 4 expenses = 24 expenses total
     * All expenses are realized events with costs, distributed over the last 6 months
     */
    private static void createDemoExpenses(EventDao eventDao, String userId, String appType, String subjectId) {
        Calendar cal = Calendar.getInstance();
        long now = System.currentTimeMillis();
        
        // Create 24 expenses (6 months x 4 expenses per month)
        // Distribute them over the last 6 months
        for (int monthOffset = 0; monthOffset < 6; monthOffset++) {
            // Go back monthOffset months from now
            cal.setTimeInMillis(now);
            cal.add(Calendar.MONTH, -monthOffset);
            
            // Set to the first day of the month
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 10);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            
            // Create 4 expenses for this month, distributed throughout the month
            for (int expenseIndex = 0; expenseIndex < 4; expenseIndex++) {
                // Distribute expenses: day 5, 12, 19, 26 of the month
                int dayOfMonth = 5 + (expenseIndex * 7);
                
                // Make sure we don't exceed the last day of the month
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (dayOfMonth > maxDay) {
                    dayOfMonth = maxDay;
                }
                
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // Randomize the hour a bit (between 9 AM and 5 PM)
                int hour = 9 + (expenseIndex * 2);
                if (hour > 17) hour = 17;
                cal.set(Calendar.HOUR_OF_DAY, hour);
                
                long expenseDate = cal.getTimeInMillis();
                
                // Create expense event (realized with cost)
                EventEntity expense = createEvent(
                    userId, 
                    appType, 
                    subjectId,
                    getExpenseTitle(appType, monthOffset, expenseIndex),
                    expenseDate,
                    true, // realized
                    getExpenseCost(appType, monthOffset, expenseIndex)
                );
                
                // Set realizedAt to the expense date
                expense.realizedAt = expenseDate;
                
                // For cars, add kilometers at event
                if ("cars".equals(appType)) {
                    // Approximate km based on how many months ago
                    expense.kilometersAtEvent = 45000.0 - (monthOffset * 1000) - (expenseIndex * 100);
                }
                
                eventDao.insert(expense);
            }
        }
        
        Log.d(TAG, "Created 24 demo expenses (6 months x 4 expenses) for " + appType);
    }
    
    /**
     * Gets expense title based on app type, month offset, and expense index
     */
    private static String getExpenseTitle(String appType, int monthOffset, int expenseIndex) {
        String[] titles;
        
        switch (appType) {
            case "family":
                titles = new String[]{
                    "Medical Consultation", "Pharmacy", "Lab Test", "Specialist Visit",
                    "Dental Cleaning", "Prescription", "X-Ray", "Physical Therapy",
                    "Vaccination", "Blood Test", "Ultrasound", "Follow-up Visit",
                    "Annual Checkup", "Medication", "Dermatology", "Cardiology",
                    "Eye Exam", "Hearing Test", "Nutritionist", "Psychologist",
                    "Emergency Visit", "Surgery", "Rehabilitation", "Insurance"
                };
                break;
                
            case "pets":
                titles = new String[]{
                    "Vet Consultation", "Vaccination", "Grooming", "Food",
                    "Medication", "Lab Test", "X-Ray", "Surgery",
                    "Dental Cleaning", "Flea Treatment", "Worm Treatment", "Checkup",
                    "Emergency Visit", "Specialist", "Supplements", "Toys",
                    "Bedding", "Leash & Collar", "Crate", "Training",
                    "Pet Insurance", "Boarding", "Daycare", "Grooming Service"
                };
                break;
                
            case "cars":
                titles = new String[]{
                    "Oil Change", "Tire Rotation", "Car Wash", "Fuel",
                    "Brake Service", "Battery Replacement", "AC Service", "Alignment",
                    "Transmission Service", "Coolant Flush", "Spark Plugs", "Air Filter",
                    "Windshield Wiper", "Headlight Bulb", "Tire Replacement", "Brake Pads",
                    "Exhaust Repair", "Radiator Service", "Timing Belt", "Battery Check",
                    "Annual Inspection", "Insurance", "Registration", "Parking"
                };
                break;
                
            case "house":
                titles = new String[]{
                    "Plumbing Repair", "Electrical Work", "HVAC Service", "Cleaning",
                    "Painting", "Roof Repair", "Window Replacement", "Door Repair",
                    "Flooring", "Appliance Repair", "Gutter Cleaning", "Landscaping",
                    "Security System", "Internet Service", "Water Bill", "Electric Bill",
                    "Gas Bill", "Property Tax", "Insurance", "Maintenance",
                    "Renovation", "Furniture", "Utilities", "Home Improvement"
                };
                break;
                
            default:
                titles = new String[]{
                    "Expense 1", "Expense 2", "Expense 3", "Expense 4",
                    "Expense 5", "Expense 6", "Expense 7", "Expense 8",
                    "Expense 9", "Expense 10", "Expense 11", "Expense 12",
                    "Expense 13", "Expense 14", "Expense 15", "Expense 16",
                    "Expense 17", "Expense 18", "Expense 19", "Expense 20",
                    "Expense 21", "Expense 22", "Expense 23", "Expense 24"
                };
        }
        
        int index = (monthOffset * 4) + expenseIndex;
        if (index >= titles.length) {
            index = index % titles.length;
        }
        return titles[index];
    }
    
    /**
     * Gets expense cost based on app type, month offset, and expense index
     * Costs vary to make the demo more realistic
     */
    private static Double getExpenseCost(String appType, int monthOffset, int expenseIndex) {
        // Base costs vary by app type
        double baseCost;
        switch (appType) {
            case "family":
                baseCost = 100.0;
                break;
            case "pets":
                baseCost = 60.0;
                break;
            case "cars":
                baseCost = 150.0;
                break;
            case "house":
                baseCost = 200.0;
                break;
            default:
                baseCost = 100.0;
        }
        
        // Vary cost based on expense index (0-3) to make it more realistic
        // Each expense in the month has a different cost
        double multiplier = 0.8 + (expenseIndex * 0.15); // 0.8, 0.95, 1.1, 1.25
        
        // Add some randomness based on month offset
        double monthVariation = 1.0 + (monthOffset * 0.05); // Slight increase for older months
        
        return Math.round((baseCost * multiplier * monthVariation) * 100.0) / 100.0;
    }
}
