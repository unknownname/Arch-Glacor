package net.botwithus

import net.botwithus.internal.scripts.ScriptDefinition
import net.botwithus.rs3.events.impl.SkillUpdateEvent
import net.botwithus.rs3.game.Client
import net.botwithus.rs3.game.Coordinate
import net.botwithus.rs3.game.Distance
import net.botwithus.rs3.game.Travel
import net.botwithus.rs3.game.actionbar.ActionBar
import net.botwithus.rs3.game.hud.interfaces.Component
import net.botwithus.rs3.game.minimenu.MiniMenu
import net.botwithus.rs3.game.minimenu.actions.ComponentAction
import net.botwithus.rs3.game.queries.builders.animations.ProjectileQuery
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery
import net.botwithus.rs3.game.queries.results.EntityResultSet
import net.botwithus.rs3.game.queries.results.ResultSet
import net.botwithus.rs3.game.scene.entities.animation.Projectile
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer
import net.botwithus.rs3.game.vars.VarManager
import net.botwithus.rs3.imgui.NativeInteger
import net.botwithus.rs3.script.Execution
import net.botwithus.rs3.script.LoopingScript
import net.botwithus.rs3.script.config.ScriptConfig
import net.botwithus.rs3.util.Regex
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery
import net.botwithus.rs3.game.scene.entities.`object`.SceneObject
import net.botwithus.rs3.util.RandomGenerator
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.*
import net.botwithus.rs3.events.impl.InventoryUpdateEvent
import net.botwithus.rs3.game.Inventory
import net.botwithus.rs3.game.Item
import net.botwithus.rs3.game.js5.types.vars.VarDomainType
import net.botwithus.rs3.game.minimenu.actions.GroundItemAction
import net.botwithus.rs3.game.queries.builders.items.GroundItemQuery
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Stream


class ArchGlacor(
    name: String,
    scriptConfig: ScriptConfig,
    scriptDefinition: ScriptDefinition
) : LoopingScript (name, scriptConfig, scriptDefinition) {

    var flicksFlicked: Int = 0
    var DevotionActive: Int = 0
    var ResonanceActive: Int = 0
    var debugMode: Boolean = true
    val GlacytePattern: Pattern = Regex.getPatternForContainsString("Glacyte")
    val Sheercold: Pattern = Regex.getPatternForContainsString("Sheer")
    var previousNpcPosition: Coordinate? = null

    var soulSplitActive: Boolean = false
    var overloadCheck: Boolean = false
    var message1: String = ""
    var matchedtext: String = ""

    var previousNpcPositionx: Int = 0
    var previousNpcPositiony: Int = 0

    //Declare types explicitly, Kotlin will use the type you specify.
    private val random: Random = Random()
    var botState: BotState = BotState.IDLE
    var radomvar: NativeInteger = NativeInteger(0)
    var xpGained: Int = 0

    var levelsGained = 0
    var startTime = System.currentTimeMillis()

    var ttl: String = "You ain't gonna level sitting around!"

    enum class BotState {
        //define your bot states here
        IDLE,
        FIGHTING,
        //etc..
    }


    override fun initialize(): Boolean {
        super.initialize()
        // Set the script graphics context to our custom one
        this.sgc = ArchGlacorGraphicsContext(this, console)

        // Use the event bus system to subscribe to SkillUpdateEvent
        // This code will fire every time a skill receives an update event from the server (level change, xp gain, etc)
        subscribe(SkillUpdateEvent::class.java) {
            xpGained += it.experience - it.oldExperience
            if (it.actualLevel - it.oldActualLevel > 0)
                levelsGained += it.actualLevel - it.oldActualLevel
        }


        subscribe(ChatMessageEvent::class.java)
        {
            //print(" First chat: ${it.message}")

            val SheerColdMessage: Pattern = Pattern.compile("</col>(.*)", Pattern.CASE_INSENSITIVE)
            val matcher = SheerColdMessage.matcher(message1)

            message1 = it.message
            if (matcher.find()) {
                matchedtext = matcher.group(1)
                println(matchedtext)
            }


        }


        // Use the event bus system to subscribe to InventoryUpdateEvent
        // This code will fire every time any inventory receives an update event from the server (item added, stack size changed, etc)
        /*subscribe(InventoryUpdateEvent::class.java) {
            // Update our statistics based on the item that was updated in the inventory.
            if (it.inventoryId == 93) {
                // Make sure we only do it for the backpack inventory, 93.
                // Otherwise, these stats could update when flowers are deposited to the bank for example (Inventory 95)
                when (it.newItem.name) {
                    "Roses" -> {
                        rosesCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Irises" -> {
                        irisCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Hydrangeas" -> {
                        hydrangeaCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Hollyhocks" -> {
                        hollyhockCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Golden roses" -> {
                        goldenRosesCollected += it.newItem.stackSize - it.oldItem.stackSize
                    }
                    "Piece of Het" -> {
                        piecesOfHet = it.newItem.stackSize - it.oldItem.stackSize
                    }
                }
            }
        }*/
        return true
    }

    // Save values that need to persist to the script configuration properties.
    /*fun saveConfiguration() {
        configuration.addProperty("bushType", bushType.get().toString())
        configuration.addProperty("botState", botState.name)
        configuration.save()
    }*/

    // Attempt to load the persistent properties from the script configuration.

    override fun onLoop() {
        // Fetch the local player from the game client
        val player = Client.getLocalPlayer()

        // If the player is null, not logged in, or our bot state is idle, we don't want to do anything.
        // Return a random delay and try again in a bit.
        if (Client.getGameState() != Client.GameState.LOGGED_IN || player == null || botState == BotState.IDLE) {
            Execution.delay(random.nextLong(2500, 5500))
            return
        }


        // At this point, we have our player, we're logged in, and our script is in a state it should be doing something.

        // Update our statistics based on values that may have changed since last onLoop iteration
        // For example, our inventory update event could have fired, and we need to update the numbers ImGui displays.
        updateStatistics()

        // Save current state
        // saveConfiguration()


        // Handle the possible bot states
        when (botState) {
            BotState.FIGHTING -> {
                // Delay  by the randomized value returned from handleSkilling, then return from onLoop
                //Execution.delay(GlacyteMech())
                //Execution.delay(monitorprojetile())
                //Execution.delay(ArchGlacorAnimation())
                Execution.delay(handleSkilling(player))
                //Execution.delay(AnimationIDFinder())


                return
            }

            BotState.IDLE -> {
                // Delay and do nothing, we're in the idle state.
                Execution.delay(random.nextLong(2500, 5500))
                return
            }
        }


    }


    private fun attackNpc(npc: Npc?) {
        npc?.interact("Attack")
        Execution.delay(1000)
    }

    private fun ShatterBalls(npc: Npc?) {
        npc?.interact("Shatter")
        println("should be interacting")
        Execution.delay(700)
    }

    private fun frostcannon(player: LocalPlayer) {
        val ArchGlacor1: ResultSet<Npc> = NpcQuery.newQuery().name("Arch-Glacor").results();
        if (ArchGlacor1.isEmpty) {
            if (debugMode)
                println(" No Arch Glacor Found Frost")
            //disablePrayer()

        }
        ArchGlacor1.forEach {
            if (debugMode)
                println("Animation ID: ${it.animationId}")
            if (it.animationId == 34278) {
                //ActionBar.usePrayer("Deflect Magic")
                handleMagicPrayerSwitch()
                var adrenalinenumber: Int = VarManager.getVarValue(VarDomainType.PLAYER, 679)

                var resonance: Component? = ComponentQuery.newQuery(284).spriteId(14222).results().first()
                var Devotion: Component? = ComponentQuery.newQuery(284).spriteId(21665).results().first()
                delay(2500)

                if (adrenalinenumber > 50 && Devotion == null) {
                    val success: Boolean = ActionBar.useAbility("Devotion")
                    println("Devotion Activated")
                    if (success) {
                        println("Devotion Activated Successfully")
                        DevotionActive++
                    }
                } else if (resonance == null) {
                    val success: Boolean = ActionBar.useAbility("Resonance")
                    println("Resonance Activated")
                    if (success) {
                        ResonanceActive++
                    }
                }

            } else {
                disablePrayer()
            }
            //println("Found Arch Glacor $ArchGlacor1 animation ID: ${it.animationId}" )   // Animation ID For Frost Cannon is 34278

        }
        return
    }   //Completed - working as intended

    private fun exposedcore()   // Completed - Working as Intended
    {
        val ArchGlacor1: ResultSet<Npc> = NpcQuery.newQuery().name("Arch-Glacor").results();
        val nearbynpc: Npc? = NpcQuery.newQuery().name("Icy Arm (left)", "Icy Arm (right)").results().nearest()
        //val targetnpc: Npc? = nearbynpc?.get(0)
        //println("Core Arm $nearbynpc")
        if (ArchGlacor1.isEmpty) {
            if (debugMode)
                println("No Arch Glacor Found")
        }
        ArchGlacor1.forEach {
            if (debugMode)
                println("Animation ID: ${it.animationId}")

            if (it.animationId == 34282) //Core is exposed
            {

                println("Core's Open Attack Arms to close it")
                //println("Core Arm Attacking $nearbynpc")
                attackNpc(nearbynpc)
                println("Name of NPC ${nearbynpc?.name} Health of NPC ${nearbynpc?.currentHealth}")
            }

        }


    }


    private fun GlacyteMech() {
        val minions: ResultSet<Npc> = NpcQuery.newQuery().name(GlacytePattern).results()
        val nearbynpc: Npc? = NpcQuery.newQuery().name(GlacytePattern).results().nearest()
        val unstablecore = NpcQuery.newQuery().name("Unstable glacyte core").results() //name("Unstable glacyte core")
        val corecount = unstablecore.size()
        val unstablecoreintreact: Npc? = NpcQuery.newQuery().name("Unstable glacyte core").results().nearest()
        if (minions.isEmpty)
            if (debugMode) {
                println("No Glacyte Found")
                //disablePrayer()

            }
        minions.forEach {
            if (debugMode)

            //println("Core Count $corecount")
            //handlePrayerFlick(it)
                attackNpc(nearbynpc)
            Execution.delay(700)
            return
        }
        if (unstablecore.isEmpty) {
            println("No core found")
        }
        unstablecore.forEach {
            if (debugMode)
            //println("Inside unstable core before intreact")
                ShatterBalls(unstablecoreintreact)
            Execution.delay(700)
            return
        }

        return
    }     // Working as Intended

    private fun CreepingIce()  // Completed - Working as Intended
    {
        val playerLocation = Client.getLocalPlayer()?.coordinate
        val creepingice: ProjectileQuery = ProjectileQuery.newQuery().id(7480)
        val creepingiceresult: EntityResultSet<Projectile> = creepingice.results()
        var currentcycle: Int = net.botwithus.rs3.game.Client.getClientCycle()
        val randomXoffSet = RandomGenerator.nextInt(4,9)
        val randomYoffSet = RandomGenerator.nextInt(-2, 2)
        for (projectile in creepingiceresult) {
            val playerPosition = playerLocation?.coordinate
            val playerPostionX = playerLocation?.coordinate?.x
            val playerPostionY = playerLocation?.coordinate?.y
            val playerPostionZ = playerLocation?.coordinate?.z
            val startCoordsX = projectile.start.x.toString()
            val startCoordsY = projectile.start.y.toString()
            val id = projectile.id
            val destinationCoordsX = projectile.destination.x
            val destinationCoordsY = projectile.destination.y
            val startCycle = projectile.startCycle
            val endCycle = projectile.endCycle
            val test = projectile.source

            /*println("Projectile ID:  $id")
            println("projectile Start Coordinates X:  $startCoordsX")
            println("projectile Start Coordinates Y:  $startCoordsY")
            println("-------------------------------")*/
            println("projectile Destination Coordinates X: $destinationCoordsX")
            //println("projectile Destination Coordinates Y: $destinationCoordsY")
            /*println("Start Cycle:: $startCycle")
            println("End Cycle:: $endCycle")
            println("-------------------------------")
            println("player Start Coordinates X :  $playerPostionX")
            println("player Start Coordinates Y :  $playerPostionY")*/


                //
            if ((playerLocation?.coordinate?.x!! > projectile.destination.x || playerLocation?.coordinate?.x!! < projectile.destination.x) && currentcycle < endCycle) {
                //println("Player X Coor $playerPostionX")
                val newx = destinationCoordsX + randomXoffSet
                val newy = playerPostionY!! + randomYoffSet
                println("newx $newx")
                println("Player X Coor $playerPostionX")
                Travel.walkTo(newx, playerPostionY)
                Execution.delay(1000)
                println("Moved to position to avoid Creeping Ice")
                println("Player X Coor after move $playerPostionX")

            }
            println("projectile Destination Coordinates After loop: $destinationCoordsX")
            //println("projectile Destination Coordinates: $destinationCoordsY")

            /*if(projectile.isInFlight(currentcycle.toLong())) {
                println("Projecting Targeting Player detected")
            }

            if(projectile.hasReachedTarget(currentcycle.toLong()))
            {
                println("Projectile Reach the Player")

            }*/

        }
        return

    }


    private fun AnimationIDFinder(): Long {
        val Flurry: ResultSet<Npc> = NpcQuery.newQuery().name("Arch-Glacor").results()
        var FlurryNum: Int = 1
        if (Flurry.isEmpty)
            if (debugMode) {
                println("No Arch-Glacor Found")
                return random.nextLong(1000,2000)
            }
        Flurry.forEach {
            if (debugMode)
                println("Glacyte Number $FlurryNum animation ID: ${it.animationId}")
            var resonance: Component? = ComponentQuery.newQuery(284).spriteId(14222).results().first()
            var ResCooldown: Int = ActionBar.getCooldown("Resonance")
            println("CoolDown Remaining $ResCooldown")

            if (resonance == null && ResCooldown == 0) {
                val success: Boolean = ActionBar.useAbility("Resonance")
                println("Resonance Activated")
                if (success) {
                    ResonanceActive++
                }
            }
            return random.nextLong(1000,2000)

        }

        return random.nextLong(1000,2000)
    }


    private fun Flurry() {

        val ArchGlacor1: ResultSet<Npc> = NpcQuery.newQuery().name("Arch-Glacor").results();
        val ArchGlacorNum: Int = 1
        if (ArchGlacor1.isEmpty) {
            if (debugMode)
                println(" No Arch Glacor Found")
            //disablePrayer()

        }
        ArchGlacor1.forEach {
            if (debugMode)
                println("Found Arch Glacor $ArchGlacorNum animation ID: ${it.animationId}")
            //println("Found Arch Glacor $ArchGlacorNum Projectile ID: ${it.headbars}")
            handlePrayerFlick(it)

        }
        return;
        //return random.nextLong(1000, 1350)
    }   // Working as Intended

    private fun SheerCold() {
        val playerLocation = Client.getLocalPlayer()?.coordinate
        val sheercold = NpcQuery.newQuery().name("Sheer cold")
        val sheerresult: EntityResultSet<Npc> = sheercold.results()

        val creepingice: ProjectileQuery = ProjectileQuery.newQuery().id(7480)
        val creepingiceresult: EntityResultSet<Projectile> = creepingice.results()
        var creepingICEx: Int = 0
        val randomXoffSet = RandomGenerator.nextInt(-5, 5)
        val randomYoffSet = RandomGenerator.nextInt(-2, 2)
        var newcord = Coordinate(playerLocation!!.x + randomXoffSet, playerLocation!!.y + randomYoffSet, 0)


        val aqueductportal = SceneObjectQuery.newQuery().name("Lobby Portal").results().nearest()
        val topleft: Coordinate = Coordinate(
            aqueductportal?.coordinate?.x?.minus(1)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        val topRight: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.minus(1)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val bottomLeft: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(30)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        val bottomRight: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(30)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val firstmiddle: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val secondmiddle: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val nearboss: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        for(projectile1 in creepingiceresult)
        {


            creepingICEx = projectile1.destination.x
        }

        for (projectile in sheerresult) {
            val currentNpcPosition = projectile?.coordinate
            val currentNpcPositionx = projectile?.coordinate?.x
            val currentNpcPositiony = projectile?.coordinate?.y
            val currentNpcPositionz = projectile?.coordinate?.z
            val playerPositionx = playerLocation?.coordinate?.x
            val playerPositiony = playerLocation?.coordinate?.y
            val playerPositionz = playerLocation?.coordinate?.z


            //println("Previous NPC Position $previousNpcPosition")
            /*println("-------------------------------------------------")
            println("Initial Previous PositionX:  $previousNpcPositionx")
            println("Initial Previous PositionY:  $previousNpcPositiony")
            println("Player PositionY:  $playerPositiony")
            println("Player PositionX:  $playerPositionx")
            println("Current NPC PositionX:  $currentNpcPositionx")
            println("Current NPC PositionY:  $currentNpcPositiony")
            println("---------------------------------------------------")*/
            println("-------------")
            println(message1)
            println("--------------")

            if( playerLocation.x < creepingICEx)
            {
                Travel.walkTo(nearboss)
            }
            if (playerLocation != null ) ///(playerPositionx != null && playerPositiony != null) && (previousNpcPositionx != 0 && previousNpcPositiony != 0)  //&& previousNpcPosition != null
            {
                var newcord = Coordinate(playerLocation!!.x + randomXoffSet, playerLocation!!.y + randomYoffSet, 0)
                println("Inside First If statement -----------------")

                // val previousDistance = previousNpcPosition!!.distanceTo(playerLocation)
                val currentDistance = currentNpcPosition?.distanceTo(playerLocation)
                println("Current Distance $currentDistance")
                //(previousNpcPositionx < currentNpcPositionx!! && previousNpcPositiony < currentNpcPositiony!!) || (previousNpcPositionx > currentNpcPositionx || previousNpcPositiony > currentNpcPositiony!!)
                if (currentDistance!! <= 4.0) {
                    Travel.walkTo(topleft)
                    delay(3000)
                    Travel.walkTo(topRight)
                    delay(3000)
                    Travel.walkTo(firstmiddle)
                    delay(3000)
                    Travel.walkTo(nearboss)
                    //Travel.walkTo(newcord)
                    Execution.delay(750)
                    println("New Coordinates Walked")
                    return
                }

                return
                //if()
                //Travel.walkTo()
            }

            previousNpcPosition = currentNpcPosition
            previousNpcPositionx = projectile?.coordinate?.x!!
            previousNpcPositiony = projectile?.coordinate?.y!!

            /*println("Previous Location = CurrentX:  $previousNpcPositionx")
            println("Previous Location = CurrentY:  $previousNpcPositiony")*/
        }

    }

    private fun TestPojectiles() {

        val PillarsofIce: EntityResultSet<Projectile>? = ProjectileQuery.newQuery().results()
        for (projectile in PillarsofIce!!) {
            val id = projectile.id
            val source = if ((projectile.source != null)) projectile.source.name else "Unknown Source"
            val target = if ((projectile.target != null)) projectile.target.name else "Unknown Target"
            val startCycle = projectile.startCycle
            val endCycle = projectile.endCycle
            val startCoords = projectile.start.toString()
            val destinationCoords = projectile.destination.toString()

            var currentcyle: Int = Client.getClientCycle()
            var inFlight: Boolean = projectile.isInFlight(currentcyle.toLong())

            println("Projectile ID:  $id")
            println("Source:  $source")
            println("Target: $target")
            println("Start Cycle: $startCycle")
            println("End Cycle:  $endCycle")
            println("Start Coordinates:  $startCoords")
            println("Destination Coordinates: $destinationCoords")
            println("Is in flight:  $inFlight")
            println("-------------------------------")

        }


        return
        //return random.nextLong(1000, 1350)
    }   // Creeping Ice  Test Projectiles

    fun checkNpcsTargetingPlayer() {
        // Assuming a method to get the player's current location
        val playerLocation = Client.getLocalPlayer()?.coordinate

        // Query NPCs around the player
        val nearbyNpcs =
            NpcQuery.newQuery().name(Sheercold).results() // Customize this query based on your needs  //War 24482

        nearbyNpcs.forEach { npc ->
            // Hypothetical method to analyze NPC behavior or state
            if (isNpcAggressiveTowardsPlayer(npc)) {
                //println("${npc.name} is targeting the player.")
                val distancetoplayer = Distance.between(playerLocation?.coordinate!!, npc.coordinate!!)
                println("${npc.name} Distance from player $distancetoplayer")
                println("NPC ID ${npc.id}")
                // Additional logic here
            }
        }
    }

    private fun isNpcAggressiveTowardsPlayer(npc: Npc): Boolean {
        val player = Client.getLocalPlayer()
        player?.let {
            // Hypothetical distance threshold for aggression
            val aggressionDistanceThreshold = 10.0 // Example threshold

            // Calculate the distance between the NPC and the player
            val distanceToPlayer = npc.coordinate?.let { it1 -> Distance.between(it.coordinate, it1) }

            // Consider the NPC aggressive if it's within the threshold distance to the player
            if (distanceToPlayer != null) {
                return distanceToPlayer <= aggressionDistanceThreshold
            }
        }
        return false
    }   //Part of Pillar of ICE NPC Traggeting Player

    private fun monitorNpcMovement() {

        val playerLocation = Client.getLocalPlayer()?.coordinate

        // Hypothetical periodic check (you need to implement the timing mechanism)
        //val npc = NpcQuery.newQuery().id(npcId).results().firstOrNull()   ///28245
        //val npc1: ResultSet<Npc> = NpcQuery.newQuery().name("War").results()


        val npc1 = NpcQuery.newQuery().name("War").results()
            .firstOrNull() // Customize this query based on your needs  //War 24482

        npc1?.let {
            val currentNpcPosition = it.coordinate
            val playerPosition = playerLocation?.coordinate

            println("Pervious NPC Postion $previousNpcPosition")
            println("Pervious Player  Postion $playerPosition")
            println("Curremt NPC Poistion $currentNpcPosition")
            if (playerPosition != null && previousNpcPosition != null) {
                val previousDistance = previousNpcPosition!!.distanceTo(playerPosition)
                val currentDistance = currentNpcPosition?.distanceTo(playerPosition)
                //println("Player Position $previousDistance")
                //println("Current NPC Position $currentDistance")


                /*if (currentDistance != null) {
                    if (currentDistance < previousDistance) {
                        println("NPC ${npc1.name} is moving towards the player.")
                    }
                }*/

            }
            previousNpcPosition = currentNpcPosition
            // Check if the NPC moved closer to the player compared to its previous position

            println("Previous NPC Position $previousNpcPosition")
        }

        fun Coordinate.distanceTo(other: Coordinate): Double {
            // Implement distance calculation (e.g., Euclidean distance)
            return Math.sqrt(
                Math.pow((this.x - other.x).toDouble(), 2.0) + Math.pow(
                    (this.y - other.y).toDouble(),
                    2.0
                )
            )
        }
    }


    private fun handlePrayerFlick(archgl: Npc) {
        /*if(archgl.animationId == 34273) {
            //Melee Prayer Active
            handleMeleePrayerSwitch()

        }*/
        if (archgl.animationId == 34275 || archgl.animationId == 34274) {
            //Range Prayer Active
            handleRangePrayerSwitch()
        } else if (archgl.animationId == 34272 || archgl.animationId == 34273) {
            //Magicc Prayer Flick
            handleMagicPrayerSwitch()
        } else {
            if (debugMode)
                println("No switch required.")
        }
    }


    private fun handleMagicPrayerSwitch() {
        println("Detected magic switch, attempting...")
        if (VarManager.getVarbitValue(16768) == 0) {
            //The varbit is 0, meaning deflect magic is not on, so we should turn it on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            val success: Boolean = ActionBar.usePrayer("Deflect Magic")
            //println("Switched to deflect magic:  $success")
            if (success) {
                //Increment the integer tracking our number of flicks for the UI stats tab.
                flicksFlicked++
                //Wait after clicking the prayer so we don't spam it and never turn it on.
                Execution.delay(random.nextLong(1550, 2050))
            }
        } else {
            //The varbit is 1, meaning deflect magic is already on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            println("Varbit 16798 was ${VarManager.getVarbitValue(16768)}")
        }
    }

    private fun handleRangePrayerSwitch() {
        println("Detected range switch, attempting...")
        if (VarManager.getVarbitValue(16769) == 0) {
            //The varbit is 0, meaning deflect ranged is not on, so we should turn it on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            val success: Boolean = ActionBar.usePrayer("Deflect Ranged")
            //Log our attempt and true/false result to the script console.
            //println("Switched to deflect ranged:  $success")
            if (success) {
                //Increment the integer tracking our number of flicks for the UI stats tab.
                flicksFlicked++
                //Wait after clicking the prayer so we don't spam it and never turn it on.
                Execution.delay(random.nextLong(1550, 2050))
            }
        } else {
            //The varbit is 1, meaning deflect ranged is already on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            println("Varbit 16769 was ${VarManager.getVarbitValue(16769)}")
        }
    }

    private fun handleMeleePrayerSwitch() {
        println("Detected range switch, attempting...")
        if (VarManager.getVarbitValue(16770) == 0) {
            //The varbit is 0, meaning deflect ranged is not on, so we should turn it on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            val success: Boolean = ActionBar.usePrayer("Deflect Melee")
            //Log our attempt and true/false result to the script console.
            //println("Switched to deflect Melee:  $success")
            if (success) {
                //Increment the integer tracking our number of flicks for the UI stats tab.
                flicksFlicked++
                //Wait after clicking the prayer so we don't spam it and never turn it on.
                Execution.delay(random.nextLong(1550, 2050))
            }
        } else {
            //The varbit is 1, meaning deflect ranged is already on.
            //Use variable debug to find these varbits, or the scripting-data channel!
            println("Varbit 16770 was ${VarManager.getVarbitValue(16770)}")
        }
    }

    private fun disablePrayer() {
        if (VarManager.getVarbitValue(16768) == 1) {
            ActionBar.usePrayer("Deflect Magic")
        } else if (VarManager.getVarbitValue(16769) == 1) {
            ActionBar.usePrayer("Deflect Ranged")
        } else if (VarManager.getVarbitValue(16770) == 1) {
            ActionBar.usePrayer("Deflect Melee")
        } else {
            //println("Prayer's are Already Disable")
        }

    }

    private fun shouldactivateSoulSplit(): Boolean {
        return LocalPlayer.LOCAL_PLAYER.inCombat()
    }

    private fun updateSoulSplitActivation() {
        var soulSplitEnabled = VarManager.getVarbitValue(16779)
        var shouldbeactivate: Boolean = shouldactivateSoulSplit()
        if (shouldbeactivate && soulSplitEnabled != 1) {
            activeSoulSplit()
        } else if (!shouldbeactivate && soulSplitEnabled == 1) {
            deactivateSoulSplit()
        }
    }

    private fun activeSoulSplit() {
        if (!soulSplitActive) {
            println("Activating Soul Split")
            if (ActionBar.useAbility("Soul Split"))
                soulSplitActive = true;
        } else {
            println("Fail to activate Soul Split")

        }

    }

    private fun deactivateSoulSplit() {
        if (soulSplitActive) {
            println("Deactivating Soul Split")
            if (ActionBar.useAbility("Soul Split"))
                soulSplitActive = false;
        } else {
            println("Fail to deactivate Soul Split")
        }

    }

    private fun isOverloadActive(): Boolean {
        var overloadActive: Boolean = VarManager.getVarbitValue(26037) != 0
        if (!overloadCheck && overloadActive) {
            overloadCheck = true
        }
        return overloadActive
    }

    private fun useOverload() {
        if (!isOverloadActive()) {
            val overloadVariants = arrayOf(
                "Elder overload potion (6)", "Elder overload potion (5)", "Elder overload potion (4)",
                "Elder overload potion (3)", "Elder overload potion (2)", "Elder overload potion (1)"
            )
            for (potionName: String in overloadVariants) {
                if (ActionBar.containsItem(potionName)) {
                    var successfulDrink: Boolean = ActionBar.useItem(potionName, "Drink")
                    if (successfulDrink) {
                        println("Drink $potionName to activate overload")
                        break
                    }
                }
            }
        }
    }

    private fun manageCombatandHealth(player: LocalPlayer) {
        if (player == null) {
            println("Player is null")
            return
        }

        var currentHealth: Int = player.currentHealth
        var maxHealth: Int = player.maximumHealth
        var healthPercent: Int = (currentHealth * 100) / maxHealth
        var prayerPoints: Int = player.prayerPoints

        if (currentHealth <= 7000) {
            println("Health is low. Eating/Drinking Food")
            ActionBar.useAbility("Eat Food")
            /*if (isOverloadActive()) {
                ActionBar.useAbility("Brew")
            } else {
                ActionBar.useAbility("Eat Food")
            }*/

            if (prayerPoints <= 5000) {
                usePrayerPotions()
            }
        }
       /* if (player.inCombat() && !soulSplitActive) {
            updateSoulSplitActivation()
        } else if (player.inCombat() && soulSplitActive) {
            deactivateSoulSplit()
        }*/
    }

    public fun usePrayerPotions() {
        var items: ResultSet<Item> = InventoryItemQuery.newQuery(93).results()

        var prayerPot: Item? = items.stream().filter { item ->
            item.name != null && item.name!!.toLowerCase().contains("prayer") || item.name!!.toLowerCase()
                .contains("restore")
        }.findFirst().orElse(null)

        if (prayerPot != null) {
            println("Drinking ${prayerPot.name}")
            var success: Boolean = MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93716713)
            Execution.delay(RandomGenerator.nextInt(1000, 1500).toLong())
        }

    }


    private fun handleSkilling(player: LocalPlayer): Long {
        //If we're moving, we're not ready to do anything. Return from function and try again in a bit.
        if (player.isMoving)
            return random.nextLong(500, 750)

        //println("Player Animation ID Before function:  ${player.animationId}")

        //println("Player Animation ID after function:  ${player.animationId}")
        var regionId = player.coordinate.regionId
        println("Region ID Before Portal Check  $regionId")
        println("Prayer Points: ${player.prayerPoints}")

        var adrenalinenumber1: Int = VarManager.getVarValue(VarDomainType.PLAYER, 679)


        //773 Loot Inventory
        if (player.coordinate.regionId == 13214) {
            val portalquery: SceneObject? = SceneObjectQuery.newQuery().name("Portal (Arch-Glacor)").results().nearest()
            val adrilnecrystal: SceneObject? = SceneObjectQuery.newQuery().name("Adrenaline crystal").results().nearest()
            val prayeraltar: SceneObject? = SceneObjectQuery.newQuery().name("Altar of War").results().nearest()

            if(prayeraltar  != null && player.prayerPoints < 8000)
            {
                prayeraltar.interact("Pray")
                Execution.delay(random.nextLong(5000, 7000))
            }
            if(adrilnecrystal != null && adrenalinenumber1 < 700)
            {
                adrilnecrystal.interact("Channel")
                Execution.delay(random.nextLong(10000, 15000))
            }

            if (portalquery != null) {
                val success: Boolean = portalquery.interact("Enter")
                //println("Interacted with Portal $success")
                if (success) {
                    return random.nextLong(300, 1200)
                }
            }
            println("War Area")
            println("First Loop ${player.coordinate.regionId}")
            return random.nextLong(1000, 1500)
        }
        if (player.coordinate.regionId == 6929) {
            val aqueductportal22 = SceneObjectQuery.newQuery().name("Aqueduct Portal").results().nearest()
            println("Portal Select Area")
            println("Second Loop ${player.coordinate.regionId}")
            if (aqueductportal22 != null) {
                println("Portal Check Loop")
                val success1: Boolean = aqueductportal22.interact("Enter")
                Execution.delay(2000)
                MiniMenu.interact(ComponentAction.DIALOGUE.type, 1, -1, 104267836)

                ///println("Interacted with Portal $success1")
                /*if (success1) {

                    val successStart: Boolean = (MiniMenu.interact(ComponentAction.DIALOGUE.type, 1, -1, 104267836))
                    println("Opening New Instance")
                    return random.nextLong(250, 950)


                }*/
                return random.nextLong(2000, 3000)
            }
            return random.nextLong(250, 950)
        }


            val ArchGlacor1: ResultSet<Npc> = NpcQuery.newQuery().name("Arch-Glacor").results();
            val ArchGlacor2: Npc? = NpcQuery.newQuery().name("Arch-Glacor").results().nearest()
            val aqueductportalinside = SceneObjectQuery.newQuery().name("Lobby Portal").results().nearest()
        val aqueductportal = SceneObjectQuery.newQuery().name("Lobby Portal").results().nearest()
        val topleft: Coordinate = Coordinate(
            aqueductportal?.coordinate?.x?.minus(1)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        val topRight: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.minus(1)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val bottomLeft: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(30)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        val bottomRight: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(30)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val firstmiddle: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val secondmiddle: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.plus(2)!!, aqueductportal.coordinate?.z!!
        )

        val nearboss: Coordinate = Coordinate(
            aqueductportal.coordinate?.x?.plus(9)!!,
            aqueductportal.coordinate?.y?.minus(4)!!, aqueductportal.coordinate?.z!!
        )

        if(ArchGlacor1.isEmpty)
               if (debugMode) {
                    println("No Arch-Glacor Found")
                    Travel.walkTo(nearboss)
                    return random.nextLong(1000,2000)
                }
            ArchGlacor1.forEach {
                if (debugMode)
                    println(" Arch Glacor Found")
                //useOverload()

                delay(1000)
                //attackNpc(ArchGlacor2)
                if (player.inCombat()) {
                    manageCombatandHealth(player)
                }
                /*if(isOverloadActive() == false)
                {
                    useOverload()
                }*/
                //Soul Split Activate
                if (VarManager.getVarbitValue(16768) == 0 || VarManager.getVarbitValue(16769) == 0 || VarManager.getVarbitValue(16770) == 0) {
                    updateSoulSplitActivation()
                }

                //
                //Mechanic configuration  Flurry
                if (ArchGlacor2 != null) {
                    if (ArchGlacor2.animationId == 34275 || ArchGlacor2.animationId == 34274) {
                        //Range Prayer Active
                        handleRangePrayerSwitch()
                    } else if (ArchGlacor2.animationId == 34272 || ArchGlacor2.animationId == 34273) {
                        //Magicc Prayer Flick
                        handleMagicPrayerSwitch()
                    } else if (ArchGlacor2.animationId == 34276 || ArchGlacor2.animationId == 34277) {
                        handleMeleePrayerSwitch()
                    }
                }
                // Frost Cannon
                if (it.animationId == 34278) {
                    //ActionBar.usePrayer("Deflect Magic")
                    handleMagicPrayerSwitch()
                    var adrenalinenumber: Int = VarManager.getVarValue(VarDomainType.PLAYER, 679)

                    var resonance: Component? = ComponentQuery.newQuery(284).spriteId(14222).results().first()
                    var Devotion: Component? = ComponentQuery.newQuery(284).spriteId(21665).results().first()
                    delay(1500)
                    //resonance == null    adrenalinenumber > 600 && Devotion == null
                    var ResCooldown: Int = ActionBar.getCooldown("Resonance")
                    if (resonance == null && ResCooldown == 0) {
                        val success: Boolean = ActionBar.useAbility("Resonance")
                        println("Resonance Activated")
                        if (success) {
                            ResonanceActive++
                        }
                    } else if (adrenalinenumber > 250 && Devotion == null) {
                        val success: Boolean = ActionBar.useAbility("Devotion")
                        println("Devotion Activated")
                        if (success) {
                            println("Devotion Activated Successfully")
                            DevotionActive++
                        }

                    }
                }
                    //GlacytMech
                    GlacyteMech()
                    //Exposed Core
                    val nearbynpcArm: Npc? =
                        NpcQuery.newQuery().name("Icy Arm (left)", "Icy Arm (right)").results().first() //, "Icy Arm (right)"
                    //Inventory = InventoryItemQuery.newQuery().results()
                    if (it.animationId == 34282) //Core is exposed
                    {
                        handleMagicPrayerSwitch()
                        println("Core's Open Attack Arms to close it")
                        println("Core Arm Attacking $nearbynpcArm")
                        attackNpc(nearbynpcArm)
                        println("Name of NPC ${nearbynpcArm?.name} Health of NPC ${nearbynpcArm?.currentHealth}")
                    }
                    ////Creeping ICE
                    //CreepingIce()
                val playerLocation = Client.getLocalPlayer()?.coordinate
                val creepingice: ProjectileQuery = ProjectileQuery.newQuery().id(7480)
                val creepingiceresult: EntityResultSet<Projectile> = creepingice.results()
                var currentcycle: Int = net.botwithus.rs3.game.Client.getClientCycle()
                val randomXoffSet = RandomGenerator.nextInt(4,9)
                for (projectile in creepingiceresult)
                {
                    val playerPostionX = playerLocation?.coordinate?.x
                    val playerPostionY = playerLocation?.coordinate?.y
                    val destinationCoordsX = projectile.destination.x
                    val destinationCoordsY = projectile.destination.y

                    val endCycle = projectile.endCycle


                    val playerPosition = playerLocation?.coordinate
                    val playerPostionZ = playerLocation?.coordinate?.z
                    val startCoordsX = projectile.start.x.toString()
                    val startCoordsY = projectile.start.y.toString()
                    val id = projectile.id
                    val startCycle = projectile.startCycle
                    val test = projectile.source.direction1
                    val test3 = projectile.source.direction2
                    val target1 = projectile.target
                    val test1 = projectile.scene

                    ////////////////
                    println("----------------------------")
                    println("Prayer Position X: $destinationCoordsX")
                    println("Prayer Position Y: $destinationCoordsY")
                    println("Start Projectile Position X: $startCoordsX")
                    println("Start Projectile Position Y: $startCoordsY")
                    println("Destination Projectile Position X: $destinationCoordsX")
                    println("Destination Projectile Position Y: $destinationCoordsY")
                    println("Projectile Source: $test")
                    println("Projectile Source: $test3")
                    println("Projectile Target: $target1")
                    println("Projectile ID: $id")
                    println("----------------------------")
                    println("Projectile Scene $test1")


                    if ((playerLocation?.coordinate?.x!! > projectile.destination.x || playerLocation?.coordinate?.x!! < projectile.destination.x) && currentcycle < endCycle) {
                        //println("Player X Coor $playerPostionX")
                        val newx = destinationCoordsX + randomXoffSet
                        //val newy = playerPostionY!! + randomYoffSet
                        println("newx $newx")
                        println("Player X Coor $playerPostionX")
                        Travel.walkTo(newx, playerPostionY!!)
                        Execution.delay(1000)
                        println("Moved to position to avoid Creeping Ice")
                        println("Player X Coor after move $playerPostionX")
                    }

                }

                    /// Pillar of ICE
                    //SheerCold()
                val sheercold = NpcQuery.newQuery().name("Sheer cold")
                val sheerresult: EntityResultSet<Npc> = sheercold.results()
                for (projectile1 in sheerresult) {
                    val currentNpcPosition = projectile1?.coordinate
                    if (playerLocation != null ) ///(playerPositionx != null && playerPositiony != null) && (previousNpcPositionx != 0 && previousNpcPositiony != 0)  //&& previousNpcPosition != null
                    {
                        var newcord = Coordinate(playerLocation!!.x + randomXoffSet, playerLocation!!.y, 0)
                        println("Inside First If statement -----------------")

                        // val previousDistance = previousNpcPosition!!.distanceTo(playerLocation)
                        val currentDistance = currentNpcPosition?.distanceTo(playerLocation)
                        println("Current Distance $currentDistance")
                        //(previousNpcPositionx < currentNpcPositionx!! && previousNpcPositiony < currentNpcPositiony!!) || (previousNpcPositionx > currentNpcPositionx || previousNpcPositiony > currentNpcPositiony!!)
                        if (currentDistance!! <= 4.0) {
                            Travel.walkTo(topleft)
                            delay(3000)
                            Travel.walkTo(topRight)
                            delay(3000)
                            Travel.walkTo(firstmiddle)
                            delay(3000)
                            Travel.walkTo(nearboss)
                            //Travel.walkTo(newcord)
                            Execution.delay(750)
                            println("New Coordinates Walked")

                        }
                }
                    }

                    ///Disable Player
                    if (!player.inCombat()) {
                        disablePrayer()
                    }


            }







            //Item Pick Up
            val groundquery = GroundItemQuery.newQuery().results().nearest()
            groundquery.let {
                println("Found Ground item: ${it!!.name} at ${it!!.coordinate}")
                if(it!!.interact("Take"))
                    println("Attempting to pick up: ${it.name}")
            }


            //Teleport Out based on kills defined

            return random.nextLong(550, 850)
        }


    private fun updateStatistics() {
        val currentTime: Long = (System.currentTimeMillis() - startTime) / 1000


        }
    }




