# Translation Guide

Since we don't have Crowdin now, we'll have to resort to doing normal PR's.
This guide will help you translate PowerGrid to be more accessible.

## Finding the english locale

This is what confuses a lot of people, since we use architectury (a mod that adds compatability with fabric and forge) we have two separate folders.
These folders each have their own en_us.json file, because the language files use datagen and that is modloading separate.

The en_us.json file is located at [`forge/src/main/generated/assets/powergrid/lang/en_us.json`](./forge/src/main/generated/assets/powergrid/lang/en_us.json).
This is the file you want to translate to your language.

## Translating

Translated files are located at [`src/main/resources/assets/powergrid/lang`](./src/main/resources/assets/powergrid/lang) and you should check if there there are any in your language. There may be outdated files of your language and if so you will have to add the new entries of `en_us.json`to `your_language.json`.

Not sure what the name of the file should be? Check out the [Minecraft Wiki](https://minecraft.wiki/w/Language#Languages) language page. Look for the name of your language and under Locale code and JE you have your filename and then append .json to the end, e.g `en_gb.json`.

