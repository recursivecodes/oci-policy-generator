# OCI Policy Generator

## Purpose

## Prerequisites

Please ensure that you have the OCI CLI [installed](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/cliconcepts.htm) and configured on the machine where you run this script.  

To test, open a command prompt and type:

```oci --version```

If this command does not work, this script will be unable to generate a policy.

You must also have Groovy and Gradle installed if you plan to edit and/or recompile (or if you just want to invoke the Groovy script directly).

## Building

Run `gradle shadowJar` to create a new jar.  

## Running

Run `java -jar /path/to/jar`, or `groovy /path/to/PolicyGenerator.groovy`.  Answer the prompts to generate a new policy, then copy/paste this into your OCI IAM Policy online or follow the prompts to create the new policy directly from this tool.

You may download the [latest release](https://github.com/recursivecodes/oci-policy-generator/releases) on GitHub if you'd just like to use the tool without downloading/building the source.