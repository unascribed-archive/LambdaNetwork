# LambdaNetwork
A compact and clean wrapper for custom payload packets in Forge. It doesn't
require Java 8, the name is simply because it's most convenient when used along
with lambdas. It works fine with inner classes.

## Depending on LambdaNetwork
LambdaNetwork is intended to be shaded into the jar, and the best way to do this
in 1.8 is using the Gradle Shadow plugin, like so:
```gradle
plugins {
	id 'com.github.johnrengelman.shadow' version '1.2.3'
}

repositories {
	maven {
		url = 'http://unascribed.com/maven/releases'
	}
}

jar {
	classifier = 'slim'
}

shadowJar {
	classifier = ''
	relocate 'com.unascribed.lambdanetwork', "me.mymod.repackage.com.unascribed.lambdanetwork"
}

reobf {
	shadowJar { mappingType = 'SEARGE' }
}

tasks.build.dependsOn reobfShadowJar

artifacts {
	archives shadowJar
}

dependencies {
	compile 'com.unascribed:lambdanetwork:1.0.0'
	shadow 'com.unascribed:lambdanetwork:1.0.0'
}
```

## Quick Start
Define a LambdaNetwork in your mod class (or elsewhere, doesn't really matter):
```java
network = LambdaNetwork.builder()
	.channel("MyMod")
	.packet("SomeCoolPacket")
		.boundTo(Side.CLIENT)
		.with(DataType.BOOLEAN, "someBoolean")
		.with(DataType.FLOAT, "theFloat")
		.handledOnMainThreadBy(token -> {
			System.out.println("someBoolean: "+token.getBoolean("someBoolean"));
			System.out.println("theFloat: "+token.getFloat("theFloat"));
		})
	.packet("AnotherCoolPacket")
		.boundTo(Side.SERVER)
		.handledBy(token -> {
			System.out.println("got empty packet");
		})
	.build();
```

Then send some packets:
```java
network.send()
	.packet("SomeCoolPacket")
	.with("someBoolean", false)
	.with("theFloat", 3.1415f)
	.toEveryone();
```

## Differences from SimpleImpl

* Cleaner API (subjective, obviously)
* Fail-fast behavior: if you use the builder wrong or create a packet that is
	missing one or more fields, you immediately get an exception instead of
	having bad things happen when the other side receives a weird packet.
* More convenient send methods, such as:
	* toAllWatching: sends the packet to everyone that can see a certain entity,
		tile entity, or block. No more giant TargetPoint declarations that don't
		even do what you really mean.
	* toAllAround: Of course, if you actually do want to send a packet to
		everyone in a radius, that works too.
	* toAllIn: Send a packet to everyone in a world, without having to unwrap
		its dimensionId.
* You don't have to write serialization/deserialization code. LambdaNetwork
	handles it all for you, including packing multiple booleans in the same
	packet into bitfields, so that 8 booleans uses 1 byte instead of 8.
