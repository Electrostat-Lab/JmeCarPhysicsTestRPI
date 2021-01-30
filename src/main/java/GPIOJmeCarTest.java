import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.ChaseCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.spi.SpiChannel;
import java.io.IOException;
/**
 * JmeCarGame using RaspiberryPi GPIO Joystick Module with the help of ADC-MCP3008 & pi4J library
 * check : JoyStickModuleLibrary:
 * pi4J GPIO from wPI native ; ${}
 *
 * */
public class GPIOJmeCarTest extends SimpleApplication {

    private BulletAppState bulletAppState;
    private VehicleControl vehicle;
    private final float brakeForce = 300f;
    private final Vector3f jumpForce = new Vector3f(0, 2000, 0);
    private final float accelerationForce = 2000.0f;


    public static void main(String[] args) {
        GPIOJmeCarTest jmeGame=new GPIOJmeCarTest();
        jmeGame.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(false);
        addSky();
        createPhysicsTestWorld(rootNode, getAssetManager(), bulletAppState.getPhysicsSpace());
        buildPlayer();
        setupKeys();
    }

    private void setupKeys() {
        JoystickModule joystickModule=new JoystickModule(SpiChannel.CS0);

        try {
            joystickModule.initializeModule(100, RaspiPin.GPIO_21, PinPullResistance.PULL_DOWN);
        } catch (IOException e) {
            e.printStackTrace();
        }
        joystickModule.registerChannel(JoystickModule.Channels.CH_0);
        joystickModule.registerChannel(JoystickModule.Channels.CH_1);
        joystickModule.registerVx(JoystickModule.Channels.CH_1);
        joystickModule.registerVy(JoystickModule.Channels.CH_0);
        /*set the listener or implement the interfaces on your class*/
        joystickModule.setOnForwardListener(valueY -> {
            valueY+=accelerationForce;
            vehicle.accelerate(valueY);
        });
        joystickModule.setOnBackwardListener(valueY -> {
            valueY+=accelerationForce;
            vehicle.accelerate(-valueY);
        });
        joystickModule.setSteerRTListener(valueX -> vehicle.steer(-valueX/80f));
        joystickModule.setSteerLTListener(valueX -> vehicle.steer(valueX/20f));
        joystickModule.setNeutralizeListener((valueX, valueY) -> {
            vehicle.clearForces();
            vehicle.brake(brakeForce);
            vehicle.accelerate(0);
            vehicle.steer(0);
        });
        joystickModule.setOnClickListener(state -> vehicle.applyImpulse(jumpForce, Vector3f.ZERO));
        joystickModule.startCollectingChannelsData();

    }



    private void addSky() {
        Geometry sky = (Geometry) SkyFactory.createSky(assetManager,assetManager.loadTexture("RocketLeauge/assets/Textures/sky.jpg"),Vector3f.UNIT_XYZ, SkyFactory.EnvMapType.EquirectMap);
        /*uses low depth level*/
        sky.getMaterial().getAdditionalRenderState().setDepthFunc(RenderState.TestFunction.LessOrEqual);
        getRootNode().attachChild(sky);
    }

    private PhysicsSpace getPhysicsSpace(){
        return bulletAppState.getPhysicsSpace();
    }

    /**
     * creates a simple physics test world with a floor, an obstacle and some test boxes
     *
     * @param rootNode where lights and geometries should be added
     * @param assetManager for loading assets
     * @param space where collision objects should be added
     */
    private void createPhysicsTestWorld(Node rootNode, AssetManager assetManager, PhysicsSpace space) {
        AmbientLight a=new AmbientLight();
        a.setColor(new ColorRGBA(0.6f, 0.7f, 0.7f, 0.2f).mult(2));

        Material soccerPlayGround = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        soccerPlayGround.setTexture("EnvMap",assetManager.loadTexture("RocketLeauge/assets/Textures/sky.jpg"));
//        soccerPlayGround.selectTechnique("PostShadow",getRenderManager());
        soccerPlayGround.setTexture("ColorMap", assetManager.loadTexture("RocketLeauge/assets/Textures/metalBareTex.jpg"));
        Spatial floorGeometry = assetManager.loadModel("RocketLeauge/assets/Scenes/SoccerPlayGround.j3o");
        floorGeometry.setMaterial(soccerPlayGround);
//        DirectionalLight directionalLight=new DirectionalLight(new Vector3f(-3,-floorGeometry.getLocalScale().getY()*4,-3).normalize());
//        directionalLight.setColor(ColorRGBA.White.mult(2f));
        floorGeometry.addLight(a);
//        rootNode.addLight(directionalLight);

        floorGeometry.setLocalTranslation(0f,-10f,0f);
        floorGeometry.setLocalScale(15f, floorGeometry.getLocalScale().getY()*4,20f);
        floorGeometry.addControl(new RigidBodyControl(CollisionShapeFactory.createMeshShape(floorGeometry),0));
        rootNode.attachChild(floorGeometry);
        space.add(floorGeometry);

        //ball sphere with mesh collision shape
        Sphere sphere = new Sphere(15, 15, 5f);
        Geometry sphereGeometry = new Geometry("Sphere", sphere);
        sphereGeometry.setMaterial(createMat(ColorRGBA.White,"RocketLeauge/assets/Textures/soccerTex.jpg", sphereGeometry));
        sphereGeometry.setLocalTranslation(0f, -5f, 0f);
        sphereGeometry.setShadowMode(RenderQueue.ShadowMode.Cast);

        RigidBodyControl ballControl=new RigidBodyControl(new SphereCollisionShape(5f), 0.5f);
        ballControl.setFriction(2f);
        ballControl.setLinearVelocity(new Vector3f(0.2f,0.2f,0.2f));
        ballControl.setRollingFriction(1f);


        sphereGeometry.addControl(ballControl);
        rootNode.attachChild(sphereGeometry);
        space.add(sphereGeometry);

//        DirectionalLightShadowRenderer dlsr=new DirectionalLightShadowRenderer(assetManager,512,1);
//        dlsr.setLight(directionalLight);
//        dlsr.setShadowIntensity(0.2f);
//        dlsr.setLambda(0.55f);
//        dlsr.setShadowCompareMode(CompareMode.Hardware);
//        dlsr.setShadowZExtend(23f);
//        dlsr.setShadowZFadeLength(8f);
//        floorGeometry.setShadowMode(RenderQueue.ShadowMode.Receive);
//        viewPort.addProcessor(dlsr);


    }

    private void buildPlayer() {
        cam.setFrustumFar(2000f);
        Material mat = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(false);
        mat.setColor("Color", ColorRGBA.Black);

        //create a compound shape and attach the BoxCollisionShape for the car body at 0,1,0
        //this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
        CompoundCollisionShape compoundShape = new CompoundCollisionShape();
        BoxCollisionShape box = new BoxCollisionShape(new Vector3f(4.2f, 0.8f, 4.5f));

        compoundShape.addChildShape(box, new Vector3f(0, 2f, 0));

        Spatial chassis = assetManager.loadModel("RocketLeauge/assets/Models/ladaCar.j3o");
        chassis.setShadowMode(RenderQueue.ShadowMode.Cast);
        chassis.setLocalScale(2.2f,2.2f,2.2f);
        chassis.setLocalTranslation(new Vector3f(0, 1.2f, 0));
        //colors
        ((Node) chassis).getChild("glass").setMaterial(createMat(ColorRGBA.Black,"",null));
        ((Node) chassis).getChild("chassis").setMaterial(createMat(ColorRGBA.randomColor(), "", chassis));
        ((Node) chassis).getChild("addOns").setMaterial(createMat(null, "RocketLeauge/assets/Textures/bronzeCopperTex.jpg",null));
        ((Node) chassis).getChild("nitro").setMaterial(createMat(new ColorRGBA(0f,0f,5f,1f), "RocketLeauge/assets/Textures/metalBareTex.jpg",null));

        ((Node) chassis).getChild("frontLight").setMaterial(createMat(ColorRGBA.White,"",null));
        ((Node) chassis).getChild("backLights").setMaterial(createMat(ColorRGBA.Red,"",null));
        ((Node) chassis).getChild("uTurns").setMaterial(createMat(ColorRGBA.Yellow,"", chassis));
        ((Node) chassis).getChild("mirrors").setMaterial(createMat(ColorRGBA.White,"",null));


        //create vehicle node
        Node vehicleNode=new Node("vehicleNode");
        vehicleNode.attachChild(chassis);
        vehicleNode.setShadowMode(RenderQueue.ShadowMode.Cast);
//        chassis.setMaterial(mat);
        vehicle = new VehicleControl(compoundShape, 600f);
        vehicleNode.addControl(vehicle);
        vehicle.setPhysicsLocation(new Vector3f(20f,5f,10f));
        //add a chaseCam tomove the cam with the object

        ChaseCamera chaseCam = new ChaseCamera(cam, vehicleNode);
        chaseCam.setDefaultDistance(-18f);
        chaseCam.registerWithInput(inputManager);
        chaseCam.setDragToRotate(true);
        //setting suspension values for wheels, this can be a bit tricky
        //see also https://docs.google.com/Doc?docid=0AXVUZ5xw6XpKZGNuZG56a3FfMzU0Z2NyZnF4Zmo&hl=en
        float stiffness =30.0f;//200=f1 car
        float compValue = 0.5f; //(should be lower than damp)
        float dampValue = 3f;
        //compression force of spring(Shock Producer)
        vehicle.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        //stretch force of spring(Shock Absorber)
        vehicle.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        vehicle.setSuspensionStiffness(stiffness);
        vehicle.setMaxSuspensionForce(FastMath.pow(2, 20));

        //Create four wheels and add them at their locations
        Vector3f wheelDirection = new Vector3f(0,-1F, 0); // was 0, -1, 0
        Vector3f wheelAxle = new Vector3f(-6, 0, 0); // was -1, 0, 0
        float radius = 0.5f;
        float restLength = 0.1f;
        float yOff = radius;
        float xOff = 4*radius;
        float zOff = 6.5f*radius;

        Cylinder wheelMesh = new Cylinder(16, 16, radius, radius * 0.5f, true);

        Node node1 = new Node("wheel 1 node");
        Geometry wheels1 = new Geometry("wheel 1", wheelMesh);
        node1.attachChild(wheels1);
        wheels1.rotate(0, FastMath.HALF_PI, 0);
        wheels1.setMaterial(mat);
        vehicle.addWheel(node1, new Vector3f(-xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);

        Node node2 = new Node("wheel 2 node");
        Geometry wheels2 = new Geometry("wheel 2", wheelMesh);
        node2.attachChild(wheels2);
        wheels2.rotate(0, FastMath.HALF_PI, 0);
        wheels2.setMaterial(mat);
        vehicle.addWheel(node2, new Vector3f(xOff, yOff, zOff),
                wheelDirection, wheelAxle, restLength, radius, true);
        Node node3;
        Node node4;
        node3 = new Node("wheel 3 node");
        Geometry wheels3 = new Geometry("wheel 3", wheelMesh);
        node3.attachChild(wheels3);
        wheels3.rotate(0, FastMath.HALF_PI, 0);
        wheels3.setMaterial(mat);
        vehicle.addWheel(node3, new Vector3f(-xOff, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);

        node4 = new Node("wheel 4 node");
        Geometry wheels4 = new Geometry("wheel 4", wheelMesh);
        node4.attachChild(wheels4);
        wheels4.rotate(0, FastMath.HALF_PI, 0);
        wheels4.setMaterial(mat);
        vehicle.addWheel(node4, new Vector3f(xOff, yOff, -zOff),
                wheelDirection, wheelAxle, restLength, radius, false);

        vehicleNode.attachChild(node1);
        vehicleNode.attachChild(node2);
        vehicleNode.attachChild(node3);
        vehicleNode.attachChild(node4);
        rootNode.attachChild(vehicleNode);

        setWheelFrictionSlip(20f);

        getPhysicsSpace().add(vehicle);
        DirectionalLight directionalLight=new DirectionalLight(new Vector3f(2,2,2).mult(50).normalize());
        directionalLight.setColor(ColorRGBA.White);
        vehicleNode.addLight(directionalLight);

    }

    private void setWheelFrictionSlip(float frictionSlip) {
        for(int nOfWheel=0;nOfWheel<vehicle.getNumWheels();nOfWheel++) {
            vehicle.getWheel(nOfWheel).setFrictionSlip(frictionSlip);
        }
    }

    private Material createMat(ColorRGBA colorRGBA, String Tex, Spatial node){
        Material material=new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        /*metalness , max is 1*/
//        material.setFloat("Metallic", 0.5f);
//        /*Roughness , 1 is the max roughnesss*/
//        material.setFloat("Roughness", 0.5f);
//        material.setFloat("EmissivePower",1.0f);
//        material.setFloat("EmissiveIntensity",2.0f);
//        material.setBoolean("HorizonFade",true);
//        material.setVector3("LightDir",new Vector3f(-0.5f,-0.5f,-0.5f).normalize());
//        material.setBoolean("BackfaceShadows",true);

        if(colorRGBA !=null){
            /*Diffuse Color*/
            material.setColor("Color", colorRGBA);
            /*Reflection color*/
//            material.setColor("Specular", colorRGBA.mult(20f));
        }
        if(Tex.length() >1){
            Texture texture=assetManager.loadTexture(Tex);
            material.setTexture("ColorMap",texture);
        }
        material.setReceivesShadows(true);
        return material;
    }

    @Override
    public void simpleUpdate(float tpf) {

    }

}
