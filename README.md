<p align="center">
    <img src="./src/main/resources/assets/powergrid/icon.png" alt="Logo" width="200">
</p>
<h1 align="center">Create: Power Grid</h1>

<p>Create: Power Grid is a mod that adds physics-based electricity simulation to the Create mod.</p>
<p>
Just like in the main mod, everything is designed to encourage creativity while introducing new challenges and obstacles.
In the end you will be the proud owner of a world-wide power grid - one that <i>hopefully</i> doesn't collapse after you
flip that one unmarked switch...
</p>
<p>
Create's in-game 'Ponder' documentation will walk you through the basic physics behind the mod and help you get started
with concepts like Ohm's law.
</p>

<p>
You can join the <a href="https://discord.gg/QQqqEnJqGz">Community Discord Server</a> if you want to discuss this mod or ask questions.
</p>

## Contributing
Reporting a bug? Make sure to test it with the latest version available. Describe the steps it takes to reproduce it and include anything that can help with resolving it (screenshots, videos, logs)
Wanting to translate the mod? Theres a guide for that! See the [TRANSLATING.md](./TRANSLATING.md) file.

## Building
To build the mod from source you need to run either `:forge:build` or `fabric:build` gradle task, your mod jar will be located in `forge/build/libs` or `fabric/build/libs`.
If you want to also build the native acceleration binary you have to have **cmake** and a working **C++ compiler** installed. Configure the cmake executable location in `native/gradle.properties`. 
