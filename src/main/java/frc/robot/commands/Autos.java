// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import frc.robot.Constants.FieldConstants;
import frc.robot.subsystems.DriveSubsystem;
import frc.robot.subsystems.IntakeSubsystem;
import frc.robot.subsystems.ShooterSubsystem;

public final class Autos {
    /** Example static factory for an autonomous command. */
    public static Command exampleAuto() {
    return Commands.none();
    }

    private Autos() {
    throw new UnsupportedOperationException("This is a utility class!");
    }

    public static String[] autoNames = {"DoNothing",  "OnlyShoot", "MoveAndShoot_StartRight", "MoveAndShoot_StartLeft", "MoveAndShoot_StartCenter"};
    
    enum StartSide {
        kLEFT, kCENTER, kRIGHT
    }
    private static StartSide m_startingSide = StartSide.kRIGHT;

    public static Command getSelectedAuto(String selectedAutoName, DriveSubsystem robotDrive, ShooterSubsystem shooter, IntakeSubsystem intake) {
        Command command = null;

        if (selectedAutoName.contains("StartRight")) {
            m_startingSide = StartSide.kRIGHT;
        } else if (selectedAutoName.contains("StartLeft")) {
            m_startingSide = StartSide.kLEFT;
        } else if (selectedAutoName.contains("StartCenter")) {
            m_startingSide = StartSide.kCENTER;
        }

        switch(selectedAutoName) {
            case "DoNothing":
            command = Commands.idle();
            break;
            case "OnlyShoot":
            command = Autos.ShootCommand(robotDrive, shooter, intake);
            break;
            case "MoveAndShoot_StartRight":
            command = Autos.moveAndShootTimeBased(robotDrive, shooter, intake);
            break;
            case "MoveAndShoot_StartLeft":
            command = Autos.moveAndShootTimeBased(robotDrive, shooter, intake);
            break;
            case "MoveAndShoot_StartCenter":
            command = Autos.moveAndShootTimeBased(robotDrive, shooter, intake);
            break;
            default:
            command = Commands.idle();
            break;
        }
        
        return command;
    }

    private static Pose2d getStartingPose() {
        // TODO: 
        // If using PathPlanner, use this starting pose to set drive Pose (for estimator). 
        // Enable using different pose for different alliance, so we know starting rotation.
        switch (m_startingSide) {
            case kLEFT:
                //return (Helpers.onRedAlliance()) ? FieldConstants.kRedLeftStart : FieldConstants.kBlueLeftStart;
                return FieldConstants.kBlueLeftStart;
            case kCENTER:
                //return (Helpers.onRedAlliance()) ? FieldConstants.kRedMiddleStart : FieldConstants.kBlueMiddleStart;
                return FieldConstants.kBlueCenterStart;
            case kRIGHT:
                //return (Helpers.onRedAlliance()) ? FieldConstants.kRedRightStart : FieldConstants.kBlueRightStart;
                return FieldConstants.kBlueRightStart;
            default:
                //return (Helpers.onRedAlliance()) ? FieldConstants.kRedRightStart : FieldConstants.kBlueRightStart;
                return FieldConstants.kBlueRightStart;
        }
    }

    private static Pose2d getShootingPose() {
        switch (m_startingSide) {
            case kLEFT:
                return FieldConstants.kBlueLeftShootingPosition;
            case kCENTER:
                return FieldConstants.kBlueCenterShootingPosition;
            case kRIGHT:
                return FieldConstants.kBlueRightShootingPosition;
            default:
                return FieldConstants.kBlueRightShootingPosition;
        }
    }

    private static Command ShootCommand(DriveSubsystem robotDrive, ShooterSubsystem shooter, IntakeSubsystem intake) {
        return Commands.parallel(
            shooter.runShooterOpenLoopCommand().withTimeout(4.0),
            Commands.sequence(
                Commands.runOnce(() -> intake.runRoller()),
                Commands.waitSeconds(1.5),
                intake.retractAuto(),
                intake.extendAuto()
            )
        ).andThen(Commands.runOnce(() -> intake.stopRoller(), intake));
    }

    public static Command moveAndShootTimeBased(DriveSubsystem robotDrive, ShooterSubsystem shooter, IntakeSubsystem intake) {
        Command driveCommand = Commands.none();
        switch (m_startingSide) {
            // TODO: Update backwards and rotational speeds and time parameter until in shooting position.
            case kLEFT:
                driveCommand = new AutonSwerveTimeControlCommand(robotDrive, -0.15, 0, -0.15, 2, true);
            break;
            case kCENTER:
                driveCommand = new AutonSwerveTimeControlCommand(robotDrive, -0.15, 0, 0, 2, true);
            break;
            case kRIGHT:
                driveCommand = new AutonSwerveTimeControlCommand(robotDrive, -0.15, 0, 0.15, 2, true);
            break;
        }
        return new SequentialCommandGroup(
            // Drive back and rotate to roughly aim and get away from trench.
            driveCommand,
            // Extend intake.
            intake.extendAuto(),
            // Shoot.
            Autos.ShootCommand(robotDrive, shooter, intake)
        );
    }

    // TODO: This command is currently unused, but it will be more useful than the time-based alternative if you plan on creating more auto routines.
    // You will have to tune the PID Controllers found in DriveSubsystem!
    public static Command moveAndShoot(DriveSubsystem robotDrive, ShooterSubsystem shooter, IntakeSubsystem intake) {
        // Assumes we are starting in line with trench and outpost.
        return new SequentialCommandGroup(
            // Turn to approx. face Hub and extend intake.
            Commands.parallel(
                new AutonSwerveDistanceControlCommand(robotDrive, getShootingPose().relativeTo(getStartingPose()).getTranslation(), getShootingPose().getRotation()),
                intake.extendAuto()
            ),
            // Shoot.
            Autos.ShootCommand(robotDrive, shooter, intake)
        );
    }
}
