# DynamicDimensions
Library to facilitate the runtime addition and removal of Minecraft dimensions.

## Adding DynamicDimensions to your project
Add the Galacticraft maven to your project
```groovy
repositories {
    maven {
        url = "https://maven.galacticraft.net/repository/maven-releases"
    }
}
```

Then add the appropriate dependency:
### Fabric
```groovy
dependencies {
    modImplementation("dev.galacticraft.dynamicdimensions-fabric:$dyndims")
}
```

### NeoForge
```groovy
dependencies {
    implementation("dev.galacticraft.dynamicdimensions-neoforge:$dyndims")
}
```

### Multiplatform
```groovy
// Common
dependencies {
    compileOnly("dev.galacticraft.dynamicdimensions-common:$dyndims")
}

// Fabric
dependencies {
    modRuntimeOnly("dev.galacticraft.dynamicdimensions-fabric:$dyndims")
}

// NeoForge
dependencies {
    runtimeOnly("dev.galacticraft.dynamicdimensions-neoforge:$dyndims")
}
```

## Using the API
All APIs dealing with the addition and removal of dynamic dimensions go through the
`dev.galacticraft.dynamicdimensions.api.DynamicDimensionRegistry`
class.

To obtain an instance, simply call 
`DynamicDimensionRegistry#from(MinecraftServer)`

Note that this library does *not* keep track of what dynamic dimensions have been created when the server restarts.
You will need to track this yourself and then
[load the dimension](#loading-a-dimension-reads-or-creates-new-level-data)
again.

### Creating a *new* dimension (overwrites level data)
Call 
`DynamicDimensionRegistry::createDynamicDimension`
with the ID of your dimension, a chunk generator, and a dimension type.

```java
ChunkGenerator generator;
DimensionType type;

// ... initialize chunk generator and dimension type ...

DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
ServerLevel level = registry.createDynamicDimension(new ResourceLocation("mymod", "dynamic"), generator, type);

if (level == null) {
    // failed to create level
} else { /*...*/ }
```

This will create a new dimension with the given ID, discarding data from any previous dimensions with the same ID.
If you want world data to be loaded, see
[the next section](#loading-a-dimension-reads-or-creates-new-level-data)
.

#### Caveats

* `createDynamicDimension` will delete all previous dimension data.
* The `DimensionType` and ID of your dimension cannot already be in use.
* There may be a one-tick delay before the dimension is registered with the server.

### Loading a dimension (reads or creates new level data)
Call 
`DynamicDimensionRegistry::loadDynamicDimension`
with the ID of your dimension, a chunk generator, and a dimension type.
```java
ChunkGenerator generator;
DimensionType type;

// ... initialize chunk generator and dimension type ...

DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
ServerLevel level = registry.loadDynamicDimension(new ResourceLocation("mymod", "dynamic"), generator, type);

if (level == null) {
    // failed to create level
} else { /*...*/ }
```

This will create a dimension with the given ID, loading previous region/level data from disk.

#### Caveats

* The `DimensionType` and ID of your dimension cannot already be in use.
* There may be a one-tick delay before the dimension is registered with the server.

### Unloading a dimension
Call 
`DynamicDimensionRegistry::unloadDynamicDimension`
with the ID of your dimension and (optionally) a callback to move connected players off-world.
```java
DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
registry.unloadDynamicDimension(new ResourceLocation("mymod", "dynamic"), null);
```
The dimension will be saved to disk before being unloaded. You can use
`loadDynamicDimension`
to create the dimension again (loading the same world data).

#### Caveats
* There may be a one-tick delay before the dimension is removed from the server.

### Deleting a dimension
Call 
`DynamicDimensionRegistry::deleteDynamicDimension`
with the ID of your dimension and (optionally) a callback to move connected players off-world.
```java
DynamicDimensionRegistry registry = DynamicDimensionRegistry.from(server);
registry.deleteDynamicDimension(new ResourceLocation("mymod", "dynamic"), null);
```
The dimension will be unloaded, then all the dimension files will be deleted.

#### Caveats

* There may be a one-tick delay before the dimension is removed from the server.
* Once deleted, dimension files are not recoverable
