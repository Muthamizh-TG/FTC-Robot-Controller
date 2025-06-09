package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp
@Config
public class FullDrive extends LinearOpMode {

    // Define motor objects for each wheel and servo
    private DcMotor frontLeft, frontRight, backLeft, backRight;
    private Servo angler;

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialize motors for chassis
        frontLeft = hardwareMap.get(DcMotor.class, "frontleft");
        frontRight = hardwareMap.get(DcMotor.class, "frontright");
        backLeft = hardwareMap.get(DcMotor.class, "backleft");
        backRight = hardwareMap.get(DcMotor.class, "backright");

        // Initialize servo
        angler = hardwareMap.get(Servo.class, "ttservo");
        angler.setPosition(0.0); // Set initial servo position to avoid unpredictable behavior

        // Set zero power behavior to BRAKE for all motors
        DcMotor[] motors = {frontLeft, frontRight, backLeft, backRight};
        for (DcMotor motor : motors) {
            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // Set motor directions for correct movement
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.REVERSE);
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);

        // Display initialization message
        telemetry.addData("Status", "Ready to start. Press Play.");
        telemetry.update();

        // Wait for the driver to press Play
        waitForStart();

        while (opModeIsActive()) {
            // Handle chassis movement using joysticks
            handleChassisMovement();

            // Handle servo control
            handleServo();

            // Update telemetry with essential data
            telemetry.addData("Motor Powers",
                    "FL: %.2f, FR: %.2f, BL: %.2f, BR: %.2f",
                    frontLeft.getPower(), frontRight.getPower(),
                    backLeft.getPower(), backRight.getPower());
            telemetry.addData("Servo Position", "%.2f", angler.getPosition());
            telemetry.update();

            // Non-blocking delay for responsiveness
            idle();
        }
    }

    /**
     * Handle servo control using gamepad buttons.
     */
    private void handleServo() {
        // Use X and B buttons to avoid conflict with precision mode (A button)
        if (gamepad1.x) {
            angler.setPosition(0.5); // Middle position
        } else if (gamepad1.b) {
            angler.setPosition(1.0); // Max position
        }
    }

    /**
     * Handle chassis movement using joystick inputs.
     */
    private void handleChassisMovement() {
        // Base multipliers for drive, strafe, and turn
        double driveMultiplier = 0.85;
        double strafeMultiplier = 0.85;
        double turnMultiplier = 0.75;

        // Turbo mode (full power) with bumpers
        if (gamepad1.right_bumper || gamepad1.left_bumper) {
            driveMultiplier = 1.0;
            strafeMultiplier = 1.0;
            turnMultiplier = 0.90;
        }
        // Precision mode (reduced power) with A button
        else if (gamepad1.a) {
            driveMultiplier = 0.4;
            strafeMultiplier = 0.4;
            turnMultiplier = 0.4;
        }

        // Get joystick inputs with exponential scaling for finer control
        double drive = -Math.pow(gamepad1.left_stick_y, 3) * driveMultiplier; // Forward/Backward
        double strafe = gamepad1.left_stick_x * strafeMultiplier; // Left/Right
        double turn = gamepad1.right_stick_x * turnMultiplier; // Turning

        // Apply deadzone to prevent joystick drift
        if (Math.abs(drive) < 0.05) drive = 0;
        if (Math.abs(strafe) < 0.05) strafe = 0;
        if (Math.abs(turn) < 0.05) turn = 0;

        // Move the robot with calculated powers
        moveRobot(strafe, drive, turn);

        // Telemetry for debugging chassis movement
        telemetry.addData("Drive", "%.2f", drive);
        telemetry.addData("Strafe", "%.2f", strafe);
        telemetry.addData("Turn", "%.2f", turn);
    }

    /**
     * Move the robot using mecanum drive calculations.
     *
     * @param x   Strafe motion (-1 to +1)
     * @param y   Forward/backward motion (-1 to +1)
     * @param yaw Turning motion (-1 to +1)
     */
    public void moveRobot(double x, double y, double yaw) {
        // Calculate wheel powers
        double leftFrontPower = y + x + yaw;
        double rightFrontPower = y - x - yaw;
        double leftBackPower = y - x + yaw;
        double rightBackPower = y + x - yaw;

        // Normalize powers to ensure they don't exceed 1.0
        double max = Math.max(1.0, Math.max(Math.abs(leftFrontPower),
                Math.max(Math.abs(rightFrontPower),
                        Math.max(Math.abs(leftBackPower), Math.abs(rightBackPower)))));

        leftFrontPower /= max;
        rightFrontPower /= max;
        leftBackPower /= max;
        rightBackPower /= max;

        // Set motor powers
        frontLeft.setPower(leftFrontPower);
        frontRight.setPower(rightFrontPower);
        backLeft.setPower(leftBackPower);
        backRight.setPower(rightBackPower);
    }
}