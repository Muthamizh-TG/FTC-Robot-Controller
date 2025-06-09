package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;


@TeleOp
@Config
public class FullDriveBackup extends LinearOpMode {

    // Define motor objects for each wheel and twin tower
    private DcMotor frontLeft, frontRight, backLeft, backRight, slide;
    private Servo angler;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize the motors for chassis and twin tower
        frontLeft = hardwareMap.get(DcMotor.class, "frontleft");
        frontRight = hardwareMap.get(DcMotor.class, "frontright");
        backLeft = hardwareMap.get(DcMotor.class, "backleft");
        backRight = hardwareMap.get(DcMotor.class, "backright");
        slide = hardwareMap.get(DcMotor.class, "slides");
        angler = hardwareMap.get(Servo.class, "angler");
        // Reset the encoder position every time the op mode is run

        slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slide.setTargetPosition(0);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setTargetPosition(0);
        slide.setPower(1);

        // Set zero power behavior for each motor to BRAKE, which helps hold the position when no power is applied
        DcMotor[] motors = {frontLeft, frontRight, backLeft, backRight, slide};
        for (DcMotor motor : motors) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // Revert motor directions to ensure correct movement of the robot
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.REVERSE);
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);

        // Display message to user to indicate that the robot is ready to start
        telemetry.addData(">", "Touch Play to start OpMode");
        telemetry.update();
        // Wait for user to start op mode
        waitForStart();

        while (opModeIsActive()) {
            // Handle chassis movement using joysticks
            handleChassisMovement();

            // Handles test servo
            handleServo();

            // Update the telemetry with current twin tower motor position
            telemetry.addData("Front Left Motor Power", frontLeft.getPower());
            telemetry.addData("Front Right Motor Power", frontRight.getPower());
            telemetry.addData("Back Left Motor Power", backLeft.getPower());
            telemetry.addData("Back Right Motor Power", backRight.getPower());
            telemetry.addData("Front Left Motor ZeroPowerBehavior", frontLeft.getZeroPowerBehavior());
            telemetry.addData("Front Right Motor ZeroPowerBehavior", frontRight.getZeroPowerBehavior());
            telemetry.addData("Back Left Motor ZeroPowerBehavior", backLeft.getZeroPowerBehavior());
            telemetry.addData("Back Right Motor ZeroPowerBehavior", backRight.getZeroPowerBehavior());
            telemetry.update();
            // Non-blocking delay to improve responsiveness
            idle();
        }
    }

    /*
      Test servo input
     */
    private void handleServo() {
        if(gamepad1.a) {
            angler.setPosition(0.5);
        }
        if(gamepad1.b) {
            angler.setPosition(1);
        }
    }

    /**
     * Handle chassis movement using joystick inputs.
     */
    private void handleChassisMovement() {
        // Define multipliers for drive, strafe, and turn rates
        double driveMultiplier = 0.85;
        double strafeMultiplier = 0.85;
        double turnMultiplier = 0.75;

        // Check if turbo mode is enabled (using bumpers)
        if (gamepad1.right_bumper || gamepad1.left_bumper) {
            driveMultiplier = 1.0; // Set drive multiplier to full power for turbo mode
            strafeMultiplier = 1.0; // Set strafe multiplier to full power for turbo mode
            turnMultiplier = 0.90;  // Set turn multiplier to higher value for faster turns
        }
        // Precision mode for fine adjustments using 'A' button
        if (gamepad1.a) {
            driveMultiplier *= 0.4; // Reduce drive multiplier for precision control
            strafeMultiplier *= 0.4; // Reduce strafe multiplier for precision control
            turnMultiplier *= 0.4;   // Reduce turn multiplier for precision control
        }

        // Get joystick input values with exponential scaling for finer control at low speeds
        double drive = -Math.pow(gamepad1.left_stick_y, 3) * driveMultiplier;  // Forward/Backward motion
        double strafe = gamepad1.left_stick_x * strafeMultiplier; // Left/Right strafing motion
        double turn = gamepad1.right_stick_x * turnMultiplier; // Curvature drive for smoother turning

        // Deadzone adjustments for joysticks to prevent drift
        if (Math.abs(drive) < 0.05) drive = 0; // Ignore small inputs for drive
        if (Math.abs(strafe) < 0.05) strafe = 0; // Ignore small inputs for strafe
        if (Math.abs(turn) < 0.05) turn = 0; // Ignore small inputs for turn


        // Move the robot using the calculated motor powers
        moveRobot(strafe, drive, turn);

        // Telemetry for chassis movement
        telemetry.addData("Drive Multiplier", driveMultiplier);
        telemetry.addData("Strafe Multiplier", strafeMultiplier);
        telemetry.addData("Turn Multiplier", turnMultiplier);
        telemetry.addData("Drive Power", drive);
        telemetry.addData("Strafe Power", strafe);
        telemetry.addData("Turn Power", turn);
        telemetry.addData("Left Stick Y", gamepad1.left_stick_y);
        telemetry.addData("Left Stick X", gamepad1.left_stick_x);
        telemetry.addData("Right Stick X", gamepad1.right_stick_x);
        telemetry.addData("Right Bumper Pressed", gamepad1.right_bumper);
        telemetry.addData("Left Bumper Pressed", gamepad1.left_bumper);
        telemetry.addData("A Button Pressed", gamepad1.a);
    }

    /**
     * Move the robot using the calculated motor powers.
     *
     * @param x   Desired strafe motion (-1 to +1)
     * @param y   Desired forward/backward motion (-1 to +1)
     * @param yaw Desired turning motion (-1 to +1)
     */
    public void moveRobot(double x, double y, double yaw) {
        // Calculate wheel powers based on the desired motions
        double leftFrontPower = y + x + yaw;
        double rightFrontPower = y - x - yaw;
        double leftBackPower = y - x + yaw;
        double rightBackPower = y + x - yaw;

        // Normalize wheel powers to ensure they do not exceed 1.0
        double max = Math.max(1.0, Math.max(Math.abs(leftFrontPower), Math.max(Math.abs(rightFrontPower), Math.max(Math.abs(leftBackPower), Math.abs(rightBackPower)))));

        leftFrontPower /= max;
        rightFrontPower /= max;
        leftBackPower /= max;
        rightBackPower /= max;

        // Set power to each motor directly without ramp-up to avoid inconsistent timing issues
        frontLeft.setPower(leftFrontPower);
        frontRight.setPower(rightFrontPower);
        backLeft.setPower(leftBackPower);
        backRight.setPower(rightBackPower);
    }
}
