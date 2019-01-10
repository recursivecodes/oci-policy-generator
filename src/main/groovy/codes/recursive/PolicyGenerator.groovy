package codes.recursive

import groovy.json.JsonSlurper

class PropertiesWithSections extends Properties {

    private String currentSection = null;

    public PropertiesWithSections(Properties defaults) {
        super(defaults);
    }

    public PropertiesWithSections() {
    }

    public void resetCurrentSection() {
        currentSection = null;
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        super.load(reader);
        resetCurrentSection();
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        super.load(inStream);
        resetCurrentSection();
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        String sKey = key.toString();
        if (sKey.startsWith("[") && sKey.endsWith("]")) {
            sKey = sKey.substring(1, sKey.length() - 1);
            if ("ROOT".equals(sKey.toUpperCase())) {
                currentSection = null;
            } else {
                currentSection = sKey;
            }
            return null;
        }
        return super.put((currentSection == null ? "" : currentSection + ".") + key, value);
    }
}

class OciPolicyGenerator {
    String ociVersion
    String policy = 'allow'
    JsonSlurper slurper = new JsonSlurper()
    String target
    List subjects = []
    List compartments = []
    List compartmentsWithRoot = []
    Map selectedCompartment
    Map selectedPolicyCompartment
    List selectedSubjects
    Map selectedVerb
    Map selectedResourceType
    Map selectedLocation
    String policyName
    String policyDescription
    Boolean policyGenerated = false

    static final def NEW_LINE = System.getProperty("line.separator")
    // static constants
    static final List TARGETS = [
            [
                    target: "any-user",
            ],
            [
                    target: "group",
            ],
    ]

    static final List LOCATIONS = [
            [
                    location: "tenancy",
            ],
            [
                    location: "compartment",
            ],
    ]

    static final List VERBS = [
            [
                    verb: "inspect",
                    type: "Ability to list resources, without access to any confidential information or user-specified metadata that may be part of that resource.${NEW_LINE}Important: The operation to list policies includes the contents of the policies themselves, and the list operations for the Networking resource-types return all the information (e.g., the contents of security lists and route tables)."
            ],
            [
                    verb: "read",
                    type: "Includes inspect plus the ability to get user-specified metadata and the actual resource itself.",
            ],
            [
                    verb: "use",
                    type: "Includes read plus the ability to work with existing resources (the actions vary by resource type). Includes the ability to update the resource, except for resource-types where the \"update\" operation has the same effective impact as the \"create\" operation (e.g., UpdatePolicy, UpdateSecurityList, etc.), in which case the \"update\" ability is available only with the manage verb. In general, this verb does not include the ability to create or delete that type of resource.",
            ],
            [
                    verb: "manage",
                    type: "Includes all permissions for the resource.",
            ],
    ]

    static final List RESOURCE_TYPES = [
            [
                    type: "all-resources",
                    includes: "(includes all shown below)",
                    family: true,
            ],
            [
                    type: "cluster-family",
                    includes: "(includes 2-4)",
                    family: true,
            ],
            [
                    type: "clusters",
            ],
            [
                    type: "cluster-node-pools",
            ],
            [
                    type: "cluster-work-requests",
            ],
            [
                    type: "database-family",
                    includes: "(includes 6-10)",
                    family: true,
            ],
            [
                    type: "db-systems",
            ],
            [
                    type: "db-nodes",
            ],
            [
                    type: "db-homes",
            ],
            [
                    type: "databases",
            ],
            [
                    type: "backups",
            ],
            [
                    type: "autonomous-transaction-processing-family",
                    includes: "(includes 12-13)",
                    family: true,
            ],
            [
                    type: "autonomous-database",
            ],
            [
                    type: "autonomous-backup",
            ],
            [
                    type: "autonomous-data-warehouse-family",
                    includes: "(includes 15-16)",
                    family: true,
            ],
            [
                    type: "autonomous-data-warehouse",
            ],
            [
                    type: "autonomous-data-warehouse-backup",
            ],
            [
                    type: "dns",
                    includes: "(includes 18-20)",
                    family: true,
            ],
            [
                    type: "dns-zones",
            ],
            [
                    type: "dns-records",
            ],
            [
                    type: "dns-traffic",
            ],
            [
                    type: "file-family",
                    includes: "(includes 22-24)",
                    family: true,
            ],
            [
                    type: "file-systems",
            ],
            [
                    type: "mount-targets",
            ],
            [
                    type: "export-sets",
            ],
            [
                    type: "instance-family",
                    includes: "(includes 26-33)",
                    family: true,
            ],
            [
                    type: "app-catalog-listing",
            ],
            [
                    type: "console-histories",
            ],
            [
                    type: "instance-configurations",
            ],
            [
                    type: "instance-console-connection",
            ],
            [
                    type: "instance-images",
            ],
            [
                    type: "instance-pools",
            ],
            [
                    type: "instances",
            ],
            [
                    type: "volume-attachments",
            ],
            [
                    type: "object-family",
                    includes: "(includes 35-37)",
                    family: true,
            ],
            [
                    type: "objectstorage-namespaces",
            ],
            [
                    type: "buckets",
            ],
            [
                    type: "objects",
            ],
            [
                    type: "virtual-network-family",
                    includes: "(includes 39-59)",
                    family: true,
            ],
            [
                    type: "vcns",
            ],
            [
                    type: "subnets",
            ],
            [
                    type: "route-tables",
            ],
            [
                    type: "security-lists",
            ],
            [
                    type: "dhcp-options",
            ],
            [
                    type: "private-ips",
            ],
            [
                    type: "public-ips",
            ],
            [
                    type: "internet-gateways",
            ],
            [
                    type: "nat-gateways",
            ],
            [
                    type: "service-gateways",
            ],
            [
                    type: "local-peering-gateways ",
            ],
            [
                    type: "remote-peering-connections",
            ],
            [
                    type: "drgs",
            ],
            [
                    type: "drg-attachments",
            ],
            [
                    type: "cpes",
            ],
            [
                    type: "ipsec-connections",
            ],
            [
                    type: "cross-connects",
            ],
            [
                    type: "cross-connect-groups",
            ],
            [
                    type: "virtual-circuits",
            ],
            [
                    type: "vnics",
            ],
            [
                    type: "vnic-attachments",
            ],
            [
                    type: "volume-family",
                    includes: "(includes 61-65)",
                    family: true,
            ],
            [
                    type: "volumes",
            ],
            [
                    type: "volume-attachments",
            ],
            [
                    type: "volume-backups",
            ],
            [
                    type: "boot-volume-backups",
            ],
            [
                    type: "backup-policies",
            ],
            [
                    type: "compartments",
            ],
            [
                    type: "users",
            ],
            [
                    type: "groups",
            ],
            [
                    type: "dynamic-groups",
            ],
            [
                    type: "policies",
            ],
            [
                    type: "identity-providers",
            ],
            [
                    type: "tenancies",
            ],
            [
                    type: "tag-namespaces",
            ],
            [
                    type: "tagdefinitions",
            ],
            [
                    type: "workrequest",
            ],
    ]

    OciPolicyGenerator() {
        addShutdownHook {
            if( !policyGenerated ) println "${NEW_LINE}Leaving so soon????"
        }
        this.ociVersion = "oci --version".execute().text
        if( !this.ociVersion ) {
            throw new Exception("You must have the OCI CLI installed locally to use this script!")
        }

        String banner = """
   ____  __________   ____        ___               ______                           __            
  / __ \\/ ____/  _/  / __ \\____  / (_)______  __   / ____/__  ____  ___  _________ _/ /_____  _____
 / / / / /    / /   / /_/ / __ \\/ / / ___/ / / /  / / __/ _ \\/ __ \\/ _ \\/ ___/ __ `/ __/ __ \\/ ___/
/ /_/ / /____/ /   / ____/ /_/ / / / /__/ /_/ /  / /_/ /  __/ / / /  __/ /  / /_/ / /_/ /_/ / /    
\\____/\\____/___/  /_/    \\____/_/_/\\___/\\__, /   \\____/\\___/_/ /_/\\___/_/   \\__,_/\\__/\\____/_/     
                                       /____/                                                      
"""
        println banner
        println "Using OCI CLI version ${ociVersion}"

    }

    def generate() {

        /* start target */
        while( !target ) {
            target = selectTarget()
        }
        buildPolicy(target)
        /* end target */

        /* start subject */
        if( target == 'any-user' ) {
            /* no further scope necessary */
        }
        else {
            subjects =  listSubjects(target)

            if( !subjects.size() ) {
                throw new Exception("There are no ${target} available to use for this policy!")
            }
            else {
                println "${NEW_LINE}Available Subjects: "
                subjects.eachWithIndex { def entry, int i ->
                    println "${i}: ${entry.name}"
                }
            }

            while( !selectedSubjects ) {
                selectedSubjects = selectSubjects()
            }
            buildPolicy(selectedSubjects*.name.join(", "))
        }
        /* end subject */

        /* start verb */
        println "${NEW_LINE}Available Verbs: "
        VERBS.eachWithIndex { def entry, int i ->
            println "${i}: ${entry.verb}"
        }
        while ( !selectedVerb ) {
            selectedVerb = selectVerb()
        }
        buildPolicy("to ${selectedVerb.verb}")
        /* end verb */

        /* start resource type */
        println "${NEW_LINE}Available Resource Types: "
        def counter = 0
        RESOURCE_TYPES.eachWithIndex { def entry, int i ->
            def isFamilyBreak = counter % 3 == 0 ? '%n' : '%n%n'

            if( entry.family ) {
                counter = 0
            }
            else {
                counter = counter + 1
            }
            System.out.format( "${entry?.family ? isFamilyBreak : ''} %-30s ${entry?.family || counter % 3 == 0 ? '%n' : ''}", "${i}: ${entry.type} ${entry?.includes ? entry.includes : ''} " )
            if( entry.type == 'backup-policies'){
                counter = 0
                println("${NEW_LINE}")
            }
        }
        println("${NEW_LINE}")
        while ( !selectedResourceType ) {
            selectedResourceType = selectResourceType()
        }
        buildPolicy("${selectedResourceType.type}")
        /* end resource type */

        /* start location */
        println "${NEW_LINE}Available Locations: "
        LOCATIONS.eachWithIndex { def entry, int i ->
            println "${i}: ${entry.location}"
        }

        while ( !selectedLocation ) {
            selectedLocation = selectLocation()
        }
        buildPolicy("in ${selectedLocation.location}")

        if( selectedLocation.location == 'compartment' ) {
            listCompartments()

            if( !compartments.size() ) {
                throw new Exception("There are no compartments available to use for this policy!")
            }
            else {
                println "${NEW_LINE}Available Compartments: "
                compartments.eachWithIndex { def entry, int i ->
                    println "${i}: ${entry.name}"
                }
            }
            while( !selectedCompartment ) {
                selectedCompartment = selectCompartment()
            }
            buildPolicy(selectedCompartment.name)
        }

        /* end location */

        /* start where */
        print("${NEW_LINE}")
        def where = System.console().readLine("For more info on conditions, see: https://docs.cloud.oracle.com/iaas/Content/Identity/Concepts/policysyntax.htm#Conditio${NEW_LINE}To add a condition, enter it as a string.${NEW_LINE}Ex: \"target.group.name != 'Administrators'\"${NEW_LINE}Leave blank for no condition(s): ")
        if( where ) {
            buildPolicy("where ${where}")
        }

        policyGenerated = true
        printPolicy()

        print("${NEW_LINE}")
        def shouldApply
        while( shouldApply == null ) {
            shouldApply = applyPolicy()
        }
        if( shouldApply ) {

            /* start policy name/description */
            while( policyName == null ) {
                policyName = enterPolicyName()
            }
            while( policyDescription == null ) {
                policyDescription = enterPolicyDescription()
            }
            /* end policy name/description */

            /* start policy compartment */
            if( !compartmentsWithRoot ) listCompartments()

            if( compartmentsWithRoot.size() ) {
                println "${NEW_LINE}Available Compartments To Apply This Policy Within: "
                compartmentsWithRoot.eachWithIndex { def entry, int i ->
                    println "${i}: ${entry.name}"
                }
            }
            while( !selectedPolicyCompartment ) {
                selectedPolicyCompartment = selectPolicyCompartment()
            }
            /* end policy compartment */

            println("${NEW_LINE} Applying policy, please wait...")
            def policyStmts = [
                    'oci', 'iam', 'policy', 'create',
                    '--compartment-id', selectedPolicyCompartment['compartment-id'],
                    '--name', policyName,
                    '--description', policyDescription,
                    '--statements', "[\"${policy}\"]"
            ]
            def policyResult = policyStmts.execute()
            def err = policyResult.err.text
            def out = policyResult.text
            if( err ) {
                System.err.println("${NEW_LINE}Error applying policy:${NEW_LINE}${err}")
            }
            else {
                println("Result: ${out}")
                println("👍 Your policy has successfully been applied to your OCI account.")
            }
        }
        else {
            println("👍 You may copy and paste the policy generated above in the OCI Identity Console to create your new policy.")
        }
    }

    def buildPolicy(str) {
        return policy += " ${str}"
    }

    def printPolicy() {
        println "${NEW_LINE}Your generated policy is:${NEW_LINE}${policy}"
    }

    def safeList(str) {
        def result = []
        def list = str.tokenize(",")
        try {
            list.each { it ->
                result << it.toInteger()
            }
        }
        catch(e) {
            result = null
        }
        return result
    }

    def safeInteger(str) {
        def result
        try {
            result = str.toInteger()
        }
        catch (e) {
            result = null
        }
        return result
    }

    def selectTarget() {
        Integer target = safeInteger( System.console().readLine("Any User (0) or Group (1)? ") )
        if( target < 0 || target > 1 ) {
            println "Please choose any-user (0) or a specific group (1)!"
            target = null
        }
        else {
            return target != null ? TARGETS[target].target : null
        }
    }

    def selectLocation() {
        Integer location = safeInteger( System.console().readLine("Tenancy (0) or Compartment (1)? ") )
        if( location < 0 || location > 1 ) {
            println "Please choose tenancy (0) or compartment (1)!"
            location = null
        }
        else {
            return location != null ? LOCATIONS[location] : null
        }
    }

    def selectVerb() {
        Integer verb = safeInteger( System.console().readLine("Select verb [0-${VERBS.size() - 1}]: " ) )
        if( verb < 0 || verb > VERBS.size() - 1 ) {
            println "Please enter a choice between 0 and ${VERBS.size() - 1}!"
            verb = null
        }
        return verb != null ? VERBS[verb] : null
    }

    def selectResourceType() {
        Integer type = safeInteger( System.console().readLine("Select resource type [0-${RESOURCE_TYPES.size() - 1}]: " ) )
        if( type < 0 || type > RESOURCE_TYPES.size() - 1 ) {
            println "Please enter a choice between 0 and ${RESOURCE_TYPES.size() - 1}!"
            type = null
        }
        return type != null ? RESOURCE_TYPES[type] : null
    }

    def selectSubjects() {
        List selections = safeList( System.console().readLine("Choose subject(s) [0-${subjects.size() - 1}] (separate multiple with a comma): ") )

        selections.each { sel ->
            if( sel < 0 || sel > subjects.size() - 1 ) {
                println "Please enter all choices between 0 and ${subjects.size() - 1}!"
                selections = null
            }
        }

        if( selections != null ) {
            def selectedSubjects = []
            selections.eachWithIndex { it, idx ->
                selectedSubjects << subjects[idx]
            }
            return selectedSubjects
        }
        else {
            return null
        }
    }

    def applyPolicy() {
        Integer selection = safeInteger( System.console().readLine("Apply Policy? Yes (1) or No (0): ") )
        if( ![0,1].contains(selection) ) {
            println "Please choose Yes (1) or No(0)!"
        }
        return selection != null ? selection == 1 : null
    }

    def enterPolicyName() {
        String pName = System.console().readLine("Enter policy name: ")
        return pName != null ? pName : null
    }

    def enterPolicyDescription() {
        String pDes = System.console().readLine("Enter policy description: ")
        return pDes != null ? pDes : null
    }

    def selectCompartment() {
        Integer selection = safeInteger( System.console().readLine("Choose compartment [0-${compartments.size() - 1}]: ") )
        if( selection < 0 || selection > compartments.size() - 1 ) {
            println "Please enter a choice between 0 and ${compartments.size() - 1}!"
        }
        return selection != null ? compartments[selection] : null
    }

    def selectPolicyCompartment() {
        Integer selection = safeInteger( System.console().readLine("Choose compartment [0-${compartmentsWithRoot.size() - 1}]: ") )
        if( selection < 0 || selection > compartmentsWithRoot.size() - 1 ) {
            println "Please enter a choice between 0 and ${compartmentsWithRoot.size() - 1}!"
        }
        return selection != null ? compartmentsWithRoot[selection] : null
    }

    def listCompartments() {
        compartments = slurper.parseText( "oci iam compartment list --all".execute().text ).data
        compartmentsWithRoot = compartments.clone()
        String path = "${System.getProperty("user.home")}/.oci/config"
        try {
            PropertiesWithSections props = new PropertiesWithSections()
            FileInputStream inputStream = new FileInputStream(path)
            inputStream.withCloseable { it ->
                props.load(it)
                def tenancyId = props.get("DEFAULT.tenancy")
                compartmentsWithRoot.push([ "name": "root", "compartment-id": tenancyId ])
            }
        }
        catch(e) {
            System.err.println("${NEW_LINE}*** Could not load OCI config, will be unable to choose 'root' compartment.  Is your config located at ${path}? ***")
        }
    }

    def listSubjects(type){
        def subjectResult = ""
        switch (type) {
            case 'user':
                subjectResult = "oci iam user list --all".execute().text
                break
            case 'group':
                subjectResult = "oci iam group list --all".execute().text
                break
        }

        subjects = slurper.parseText( subjectResult ).data
    }

}

OciPolicyGenerator generator = new OciPolicyGenerator()
generator.generate()