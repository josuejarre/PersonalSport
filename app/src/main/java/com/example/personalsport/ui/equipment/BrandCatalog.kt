package com.example.personalsport.ui.equipment

data class CatalogItem(
    val brand: String,
    val name: String,
    val category: String,
    val description: String
)

object BrandCatalog {
    val supportedBrands = listOf(
        "Technogym",
        "Life Fitness",
        "Matrix",
        "Fit4Life",
        "Oss Fitness",
        "BH Fitness",
        "Bodytone",
        "Salter",
        "Etenon",
        "Decathlon",
        "Cecotec"
    )

    val catalogItems = listOf(
        // Technogym
        CatalogItem("Technogym", "Skillrun Treadmill", "Cardio", "Commercial high-performance treadmill for running intervals"),
        CatalogItem("Technogym", "Selection Leg Extension", "Machine", "Targeted quadriceps isolation machine"),
        CatalogItem("Technogym", "Selection Chest Press", "Machine", "Guided upper body press machine"),
        CatalogItem("Technogym", "Kinesis Station Cable", "Cable", "3D multi-planar functional cable station"),
        CatalogItem("Technogym", "Skillbike Indoor Cycle", "Cardio", "Real feeling indoor road bike trainer"),

        // Life Fitness
        CatalogItem("Life Fitness", "Cable Crossover Dual Pulley", "Cable", "Adjustable dual pulley station for full body cable work"),
        CatalogItem("Life Fitness", "Hammer Strength Iso-Chest Press", "Free Weights", "Plate-loaded chest press for maximum hypertrophy"),
        CatalogItem("Life Fitness", "Integrity Series Treadmill", "Cardio", "Heavy duty cardio running platform"),
        CatalogItem("Life Fitness", "Leg Press Machine", "Machine", "45-degree angled leg press station"),

        // Matrix
        CatalogItem("Matrix", "Performance Treadmill", "Cardio", "Precision engineered commercial treadmill"),
        CatalogItem("Matrix", "Aura Seated Leg Curl", "Machine", "Ergonomic hamstring curling machine"),
        CatalogItem("Matrix", "Connexus Functional Trainer", "Cable", "Versatile functional training rig"),
        CatalogItem("Matrix", "ClimbMill Step Machine", "Cardio", "High intensity stair climbing simulator"),

        // Fit4Life
        CatalogItem("Fit4Life", "Commercial Power Cage Rack", "Free Weights", "Heavy duty power cage with pull-up bar for squats"),
        CatalogItem("Fit4Life", "Functional Trainer Tower", "Cable", "Compact dual cable tower with multi-grips"),
        CatalogItem("Fit4Life", "Bumper Plate & Olympic Barbell Set", "Free Weights", "50mm Olympic barbell with rubber bumper plates"),

        // Oss Fitness
        CatalogItem("Oss Fitness", "Heavy Duty Adjustable Bench", "Free Weights", "Flat/Incline/Decline heavy duty weight bench"),
        CatalogItem("Oss Fitness", "Plate-Loaded Hack Squat", "Machine", "Guided lower body hack squat machine"),
        CatalogItem("Oss Fitness", "Olympic Barbell & Bumper Set", "Free Weights", "20kg Olympic competition bar with weight plates"),

        // BH Fitness
        CatalogItem("BH Fitness", "Duke Indoor Cycling Bike", "Cardio", "Magnetic resistance indoor cycle"),
        CatalogItem("BH Fitness", "Movemia Treadmill", "Cardio", "Interactive touchscreen treadmill"),
        CatalogItem("BH Fitness", "L800 Multi-Gym Station", "Machine", "All-in-one selectorized stack machine"),

        // Bodytone
        CatalogItem("Bodytone", "Active Dual Pulley Station", "Cable", "Smooth cable pulley system for functional training"),
        CatalogItem("Bodytone", "Evocardio Treadmill", "Cardio", "High-cushioning professional treadmill"),
        CatalogItem("Bodytone", "Evolution Lat Pulldown", "Machine", "Isolated lat pulldown cable machine"),

        // Salter
        CatalogItem("Salter", "Pro Dumbbell Rack (5-30kg)", "Free Weights", "Complete pair set of rubber hexagonal dumbbells"),
        CatalogItem("Salter", "M-550 Magnetic Bike", "Cardio", "Smooth magnetic spin bike for endurance"),
        CatalogItem("Salter", "Multi-Gym Strength Station", "Machine", "Multi-station gym stack for home or studio"),

        // Etenon
        CatalogItem("Etenon", "Fitness Smith Machine", "Machine", "Counterbalanced guided Smith barbell station"),
        CatalogItem("Etenon", "Seated Calf Raise", "Machine", "Plate-loaded soleus & calf machine"),
        CatalogItem("Etenon", "Multi-Adjustable Utility Bench", "Free Weights", "Commercial adjustable incline bench"),

        // Decathlon
        CatalogItem("Decathlon", "Domyos Cast Iron Kettlebells", "Free Weights", "Range of 8kg to 24kg cast iron kettlebells"),
        CatalogItem("Decathlon", "Corength Squat & Pull-Up Rack", "Calisthenics", "Modular home gym rack for squats and pullups"),
        CatalogItem("Decathlon", "Resistance Band Set (15-60kg)", "Accessories", "Latex resistance loops for warmups and assistance"),
        CatalogItem("Decathlon", "Domyos Woodbike", "Cardio", "Eco-designed indoor fitness bike"),

        // Cecotec
        CatalogItem("Cecotec", "DrumFit Indoor Spin Bike", "Cardio", "Silenced flywheel indoor cycling bike"),
        CatalogItem("Cecotec", "ExtremeTrack Foldable Treadmill", "Cardio", "Foldable home running treadmill"),
        CatalogItem("Cecotec", "BodyFit Rowing Machine", "Cardio", "Water-resistance indoor rowing simulator")
    )
}
