package us.ihmc.exampleSimulations.agileHexapod;

import us.ihmc.graphicsDescription.HeightMap;
import us.ihmc.simulationconstructionset.util.RobotController;
import us.ihmc.yoVariables.registry.YoRegistry;
import us.ihmc.yoVariables.variable.YoDouble;

public class AgileHexapodController extends AgileHexapodControllerBase implements RobotController
{
   private HeightMap profile;

   private VirtualBodyForces virtualBodyForces;

   private static final int RAMP = 100;
   private static final int CIRCLE = 200;
   private static final int TRACK = 300;
   private static final int DANCE = 400;
   private static final int SETUP_DANCE = 500;
   private static final int ZIGZAG = 600;

   private static final int DOUBLE_SUPPORT = 0;
   private static final int A_SUPPORT_B_LIFT = 10;
   private static final int A_SUPPORT_B_SWING = 20;
   private static final int A_SUPPORT_B_DOWN = 30;
   private static final int B_SUPPORT_A_LIFT = 40;
   private static final int B_SUPPORT_A_SWING = 50;
   private static final int B_SUPPORT_A_DOWN = 60;

   private final YoRegistry registry = new YoRegistry("AgileHexapodController");

   public YoDouble q_pitch = new YoDouble("q_pitch", registry), q_roll = new YoDouble("q_roll", registry), q_yaw = new YoDouble("q_yaw", registry);
   public YoDouble qd_pitch = new YoDouble("qd_pitch", registry), qd_roll = new YoDouble("qd_roll", registry),
                     qd_yaw = new YoDouble("qd_yaw", registry);

   private YoDouble state = new YoDouble("state", registry), walk_state = new YoDouble("walk_state", registry);
   private YoDouble vel_ang = new YoDouble("vel_ang", registry), vel_mag = new YoDouble("vel_mag", registry),
                      vel_ramp = new YoDouble("vel_ramp", registry), vel_off = new YoDouble("vel_off", registry);

   private YoDouble e_ramp = new YoDouble("e_ramp", registry);

   private YoDouble poda_x = new YoDouble("poda_x", registry);
   private YoDouble poda_y = new YoDouble("poda_y", registry);
   private YoDouble poda_theta = new YoDouble("poda_theta", registry);
   private YoDouble poda_mag = new YoDouble("poda_mag", registry);
   private YoDouble poda_h = new YoDouble("poda_h", registry);

   private YoDouble podb_x = new YoDouble("podb_x", registry);
   private YoDouble podb_y = new YoDouble("podb_y", registry);
   private YoDouble podb_theta = new YoDouble("podb_theta", registry);
   private YoDouble podb_mag = new YoDouble("podb_mag", registry);
   private YoDouble podb_h = new YoDouble("podb_h", registry);

   private YoDouble pod_thresh_up = new YoDouble("pod_thresh_up", registry);
   private YoDouble pod_thresh_down = new YoDouble("pod_thresh_down", registry);

   private YoDouble tripod_xa_d = new YoDouble("tripod_xa_d", registry);
   private YoDouble tripod_ya_d = new YoDouble("tripod_ya_d", registry);
   private YoDouble tripod_za_d = new YoDouble("tripod_za_d", registry);
   private YoDouble tripod_thetaa_d = new YoDouble("tripod_thetaa_d", registry);

   private YoDouble tripod_xb_d = new YoDouble("tripod_xb_d", registry);
   private YoDouble tripod_yb_d = new YoDouble("tripod_yb_d", registry);
   private YoDouble tripod_zb_d = new YoDouble("tripod_zb_d", registry);
   private YoDouble tripod_thetab_d = new YoDouble("tripod_thetab_d", registry);

   private YoDouble switch_time = new YoDouble("switch_time", registry);
   private YoDouble double_time = new YoDouble("double_time", registry);
   private YoDouble swing_time = new YoDouble("swing_time", registry);

   private YoDouble lift_up = new YoDouble("lift_up", registry);
   private YoDouble ground_height = new YoDouble("ground_height", registry);
   private YoDouble alpha = new YoDouble("alpha", registry);
   private YoDouble beta = new YoDouble("beta", registry);

   private YoDouble dfoot_z_d = new YoDouble("dfoot_z_d", registry);
   private YoDouble dfoot_z1_d = new YoDouble("dfoot_z1_d", registry);
   private YoDouble dfoot_z2_d = new YoDouble("dfoot_z2_d", registry);
   private YoDouble dfoot_z3_d = new YoDouble("dfoot_z3_d", registry);
   private YoDouble dfoot_z4_d = new YoDouble("dfoot_z4_d", registry);
   private YoDouble dfoot_z5_d = new YoDouble("dfoot_z5_d", registry);
   private YoDouble dfoot_z6_d = new YoDouble("dfoot_z6_d", registry);

   private YoDouble proja_h_x = new YoDouble("proja_h_x", registry);
   private YoDouble proja_h_y = new YoDouble("proja_h_y", registry);
   private YoDouble proja_h_theta = new YoDouble("proja_h_theta", registry);
   private YoDouble ha_hat_x = new YoDouble("ha_hat_x", registry);
   private YoDouble ha_hat_y = new YoDouble("ha_hat_y", registry);
   private YoDouble ha_hat_theta = new YoDouble("ha_hat_theta", registry);

   private YoDouble projb_h_x = new YoDouble("projb_h_x", registry);
   private YoDouble projb_h_y = new YoDouble("projb_h_y", registry);
   private YoDouble projb_h_theta = new YoDouble("projb_h_theta", registry);
   private YoDouble hb_hat_x = new YoDouble("hb_hat_x", registry);
   private YoDouble hb_hat_y = new YoDouble("hb_hat_y", registry);
   private YoDouble hb_hat_theta = new YoDouble("hb_hat_theta", registry);

   private YoDouble pend_x = new YoDouble("pend_x", registry), pend_y = new YoDouble("pend_y", registry), pend_dx = new YoDouble("pend_dx", registry),
                      pend_dy = new YoDouble("pend_dy", registry);
   private YoDouble fXa = new YoDouble("fXa", registry), fYa = new YoDouble("fYa", registry), fZa = new YoDouble("fZa", registry),
                      tXa = new YoDouble("tXa", registry), tYa = new YoDouble("tYa", registry), tZa = new YoDouble("tZa", registry);
   private YoDouble fXb = new YoDouble("fXb", registry), fYb = new YoDouble("fYb", registry), fZb = new YoDouble("fZb", registry),
                      tXb = new YoDouble("tXb", registry), tYb = new YoDouble("tYb", registry), tZb = new YoDouble("tZb", registry);
   private YoDouble fX = new YoDouble("fX", registry), fY = new YoDouble("fY", registry), fZ = new YoDouble("fZ", registry),
                      tX = new YoDouble("tX", registry), tY = new YoDouble("tY", registry), tZ = new YoDouble("tZ", registry);
   private YoDouble ffZa = new YoDouble("ffZa", registry), ffZb = new YoDouble("ffZb", registry);

   private YoDouble PK1 = new YoDouble("PK1", registry), PK2 = new YoDouble("PK2", registry), PK3 = new YoDouble("PK3", registry),
                      PK4 = new YoDouble("PK4", registry);

   private YoDouble q_d_x = new YoDouble("q_d_x", registry);
   private YoDouble q_d_y = new YoDouble("q_d_y", registry);
   private YoDouble q_d_z = new YoDouble("q_d_z", registry);
   private YoDouble q_d_yaw = new YoDouble("q_d_yaw", registry);
   private YoDouble q_d_pitch = new YoDouble("q_d_pitch", registry);
   private YoDouble q_d_roll = new YoDouble("q_d_roll", registry);

   private YoDouble forward_angle = new YoDouble("forward_angle", registry);

   private YoDouble vel_d_x = new YoDouble("vel_d_x", registry), vel_d_y = new YoDouble("vel_d_y", registry), vel_x = new YoDouble("vel_x", registry),
                      vel_y = new YoDouble("vel_y", registry);
   private YoDouble qd_d_yaw = new YoDouble("qd_d_yaw", registry);
   private YoDouble hug_parallel = new YoDouble("hug_parallel", registry);

   private YoDouble x_damp = new YoDouble("x_damp", registry), x_gain = new YoDouble("x_gain", registry);
   private YoDouble y_damp = new YoDouble("y_damp", registry), y_gain = new YoDouble("y_gain", registry);
   private YoDouble z_damp = new YoDouble("z_damp", registry), z_gain = new YoDouble("z_gain", registry);
   private YoDouble yaw_damp = new YoDouble("yaw_damp", registry), yaw_gain = new YoDouble("yaw_gain", registry);
   private YoDouble pitch_damp = new YoDouble("pitch_damp", registry), pitch_gain = new YoDouble("pitch_gain", registry);
   private YoDouble roll_damp = new YoDouble("roll_damp", registry), roll_gain = new YoDouble("roll_gain", registry);

   private YoDouble foot_x1 = new YoDouble("foot_x1", registry), foot_y1 = new YoDouble("foot_y1", registry),
                      foot_z1 = new YoDouble("foot_z1", registry);
   private YoDouble foot_x2 = new YoDouble("foot_x2", registry), foot_y2 = new YoDouble("foot_y2", registry),
                      foot_z2 = new YoDouble("foot_z2", registry);
   private YoDouble foot_x3 = new YoDouble("foot_x3", registry), foot_y3 = new YoDouble("foot_y3", registry),
                      foot_z3 = new YoDouble("foot_z3", registry);
   private YoDouble foot_x4 = new YoDouble("foot_x4", registry), foot_y4 = new YoDouble("foot_y4", registry),
                      foot_z4 = new YoDouble("foot_z4", registry);
   private YoDouble foot_x5 = new YoDouble("foot_x5", registry), foot_y5 = new YoDouble("foot_y5", registry),
                      foot_z5 = new YoDouble("foot_z5", registry);
   private YoDouble foot_x6 = new YoDouble("foot_x6", registry), foot_y6 = new YoDouble("foot_y6", registry),
                      foot_z6 = new YoDouble("foot_z6", registry);

   private YoDouble dfoot_x1 = new YoDouble("dfoot_x1", registry), dfoot_y1 = new YoDouble("dfoot_y1", registry),
                      dfoot_z1 = new YoDouble("dfoot_z1", registry);
   private YoDouble dfoot_x2 = new YoDouble("dfoot_x2", registry), dfoot_y2 = new YoDouble("dfoot_y2", registry),
                      dfoot_z2 = new YoDouble("dfoot_z2", registry);
   private YoDouble dfoot_x3 = new YoDouble("dfoot_x3", registry), dfoot_y3 = new YoDouble("dfoot_y3", registry),
                      dfoot_z3 = new YoDouble("dfoot_z3", registry);
   private YoDouble dfoot_x4 = new YoDouble("dfoot_x4", registry), dfoot_y4 = new YoDouble("dfoot_y4", registry),
                      dfoot_z4 = new YoDouble("dfoot_z4", registry);
   private YoDouble dfoot_x5 = new YoDouble("dfoot_x5", registry), dfoot_y5 = new YoDouble("dfoot_y5", registry),
                      dfoot_z5 = new YoDouble("dfoot_z5", registry);
   private YoDouble dfoot_x6 = new YoDouble("dfoot_x6", registry), dfoot_y6 = new YoDouble("dfoot_y6", registry),
                      dfoot_z6 = new YoDouble("dfoot_z6", registry);

   private YoDouble foot_x1_d = new YoDouble("foot_x1_d", registry), foot_y1_d = new YoDouble("foot_y1_d", registry),
                      foot_z1_d = new YoDouble("foot_z1_d", registry);
   private YoDouble foot_x2_d = new YoDouble("foot_x2_d", registry), foot_y2_d = new YoDouble("foot_y2_d", registry),
                      foot_z2_d = new YoDouble("foot_z2_d", registry);
   private YoDouble foot_x3_d = new YoDouble("foot_x3_d", registry), foot_y3_d = new YoDouble("foot_y3_d", registry),
                      foot_z3_d = new YoDouble("foot_z3_d", registry);
   private YoDouble foot_x4_d = new YoDouble("foot_x4_d", registry), foot_y4_d = new YoDouble("foot_y4_d", registry),
                      foot_z4_d = new YoDouble("foot_z4_d", registry);
   private YoDouble foot_x5_d = new YoDouble("foot_x5_d", registry), foot_y5_d = new YoDouble("foot_y5_d", registry),
                      foot_z5_d = new YoDouble("foot_z5_d", registry);
   private YoDouble foot_x6_d = new YoDouble("foot_x6_d", registry), foot_y6_d = new YoDouble("foot_y6_d", registry),
                      foot_z6_d = new YoDouble("foot_z6_d", registry);

   private YoDouble x_max = new YoDouble("x_max", registry), y_max = new YoDouble("y_max", registry), theta_max = new YoDouble("theta_max", registry);


   private YoDouble leg_x_off = new YoDouble("leg_x_off", registry), leg_y_off = new YoDouble("leg_y_off", registry);

   private YoDouble Ffoot_x1 = new YoDouble("Ffoot_x1", registry), Ffoot_y1 = new YoDouble("Ffoot_y1", registry),
                      Ffoot_z1 = new YoDouble("Ffoot_z1", registry);
   private YoDouble Ffoot_x2 = new YoDouble("Ffoot_x2", registry), Ffoot_y2 = new YoDouble("Ffoot_y2", registry),
                      Ffoot_z2 = new YoDouble("Ffoot_z2", registry);
   private YoDouble Ffoot_x3 = new YoDouble("Ffoot_x3", registry), Ffoot_y3 = new YoDouble("Ffoot_y3", registry),
                      Ffoot_z3 = new YoDouble("Ffoot_z3", registry);
   private YoDouble Ffoot_x4 = new YoDouble("Ffoot_x4", registry), Ffoot_y4 = new YoDouble("Ffoot_y4", registry),
                      Ffoot_z4 = new YoDouble("Ffoot_z4", registry);
   private YoDouble Ffoot_x5 = new YoDouble("Ffoot_x5", registry), Ffoot_y5 = new YoDouble("Ffoot_y5", registry),
                      Ffoot_z5 = new YoDouble("Ffoot_z5", registry);
   private YoDouble Ffoot_x6 = new YoDouble("Ffoot_x6", registry), Ffoot_y6 = new YoDouble("Ffoot_y6", registry),
                      Ffoot_z6 = new YoDouble("Ffoot_z6", registry);

   private YoDouble K_leg = new YoDouble("K_leg", registry), Kz_leg = new YoDouble("Kz_leg", registry);
   private YoDouble B_leg = new YoDouble("B_leg", registry), Bz_leg = new YoDouble("Bz_leg", registry);


   public YoDouble ff_hip1_x = new YoDouble("ff_hip1_x", registry), ff_hip1_z = new YoDouble("ff_hip1_z", registry),
                     ff_knee1 = new YoDouble("ff_knee1", registry);
   public YoDouble ff_hip2_x = new YoDouble("ff_hip2_x", registry), ff_hip2_z = new YoDouble("ff_hip2_z", registry),
                     ff_knee2 = new YoDouble("ff_knee2", registry);
   public YoDouble ff_hip3_x = new YoDouble("ff_hip3_x", registry), ff_hip3_z = new YoDouble("ff_hip3_z", registry),
                     ff_knee3 = new YoDouble("ff_knee3", registry);
   public YoDouble ff_hip4_x = new YoDouble("ff_hip4_x", registry), ff_hip4_z = new YoDouble("ff_hip4_z", registry),
                     ff_knee4 = new YoDouble("ff_knee4", registry);
   public YoDouble ff_hip5_x = new YoDouble("ff_hip5_x", registry), ff_hip5_z = new YoDouble("ff_hip5_z", registry),
                     ff_knee5 = new YoDouble("ff_knee5", registry);
   public YoDouble ff_hip6_x = new YoDouble("ff_hip6_x", registry), ff_hip6_z = new YoDouble("ff_hip6_z", registry),
                     ff_knee6 = new YoDouble("ff_knee6", registry);

   public YoDouble fx1 = new YoDouble("fx1", registry), fy1 = new YoDouble("fy1", registry), fz1 = new YoDouble("fz1", registry);
   public YoDouble tx1 = new YoDouble("tx1", registry), ty1 = new YoDouble("ty1", registry), tz1 = new YoDouble("tz1", registry);
   public YoDouble fx2 = new YoDouble("fx2", registry), fy2 = new YoDouble("fy2", registry), fz2 = new YoDouble("fz2", registry);
   public YoDouble tx2 = new YoDouble("tx2", registry), ty2 = new YoDouble("ty2", registry), tz2 = new YoDouble("tz2", registry);
   public YoDouble fx3 = new YoDouble("fx3", registry), fy3 = new YoDouble("fy3", registry), fz3 = new YoDouble("fz3", registry);
   public YoDouble tx3 = new YoDouble("tx3", registry), ty3 = new YoDouble("ty3", registry), tz3 = new YoDouble("tz3", registry);
   public YoDouble fx4 = new YoDouble("fx4", registry), fy4 = new YoDouble("fy4", registry), fz4 = new YoDouble("fz4", registry);
   public YoDouble tx4 = new YoDouble("tx4", registry), ty4 = new YoDouble("ty4", registry), tz4 = new YoDouble("tz4", registry);
   public YoDouble fx5 = new YoDouble("fx5", registry), fy5 = new YoDouble("fy5", registry), fz5 = new YoDouble("fz5", registry);
   public YoDouble tx5 = new YoDouble("tx5", registry), ty5 = new YoDouble("ty5", registry), tz5 = new YoDouble("tz5", registry);
   public YoDouble fx6 = new YoDouble("fx6", registry), fy6 = new YoDouble("fy6", registry), fz6 = new YoDouble("fz6", registry);
   public YoDouble tx6 = new YoDouble("tx6", registry), ty6 = new YoDouble("ty6", registry), tz6 = new YoDouble("tz6", registry);

   private String name;

   public AgileHexapodController(AgileHexapodRobot rob, HeightMap profile, String name)
   {
      super(rob);
      this.name = name;

      this.profile = profile;
      this.virtualBodyForces = new VirtualBodyForces(rob, this);

      initVars();
   }

   public void measure_terrain()
   {
      double cosy, siny, llf;

      ground_height.set(profile.heightAt(q_x.getDoubleValue(), q_y.getDoubleValue(), q_z.getDoubleValue()));

      llf = AgileHexapodRobot.BODY_X / 2.0;

      cosy = Math.cos(q_yaw.getDoubleValue());
      siny = Math.sin(q_yaw.getDoubleValue());

      double forward_slope = (profile.heightAt(q_x.getDoubleValue() + llf * cosy, q_y.getDoubleValue() + llf * siny, q_z.getDoubleValue())
                              - profile.heightAt(q_x.getDoubleValue() - llf * cosy, q_y.getDoubleValue() - llf * siny, q_z.getDoubleValue())) / (2.0 * llf);

      forward_angle.set(Math.atan(forward_slope));
   }


   public void doControl()
   {
      q_yaw.set(Math.atan2(2.0 * q_qx.getDoubleValue() * q_qy.getDoubleValue() + 2.0 * q_qz.getDoubleValue() * q_qs.getDoubleValue(), 1.0 - 2.0 * q_qy.getDoubleValue() * q_qy.getDoubleValue() - 2.0 * q_qz.getDoubleValue() * q_qz.getDoubleValue()));    // Math.asin(q_qs.val * q_qz.val * 2.0);
      q_pitch.set(Math.asin(-2.0 * q_qx.getDoubleValue() * q_qz.getDoubleValue() + 2.0 * q_qs.getDoubleValue() * q_qy.getDoubleValue()));    // Math.asin(q_qs.val * q_qy.val * 2.0);
      q_roll.set(Math.atan2(2.0 * q_qy.getDoubleValue() * q_qz.getDoubleValue() + 2.0 * q_qx.getDoubleValue() * q_qs.getDoubleValue(), 1.0 - 2.0 * q_qx.getDoubleValue() * q_qx.getDoubleValue() - 2.0 * q_qy.getDoubleValue() * q_qy.getDoubleValue()));    // Math.asin(q_qs.val * q_qx.val * 2.0);

      qd_yaw.set(qd_wz.getDoubleValue());
      qd_pitch.set(qd_wy.getDoubleValue());
      qd_roll.set(qd_wx.getDoubleValue());

      measure_terrain();

      switch ((int) (state.getDoubleValue()))
      {
         case RAMP :    /* 100 */

            vel_ang.set(0.0);
            vel_mag.set(vel_ramp.getDoubleValue() * t.getDoubleValue() + vel_off.getDoubleValue());

            break;


         case CIRCLE :    /* 200 */

            vel_ang.set(0.0);

            // q_d_yaw.val = t.val * yaw_ramp.val + yaw_off.val;

            break;

         case TRACK :    /* 300 */

            // q_d_yaw.val = atan2((camera_y.val - q_y.val),(camera_x.val - q_x.val));
            // vel_ang.val = -q_yaw.val;

            break;

         case SETUP_DANCE :    /* 500 */

            vel_mag.set(0.0);
            vel_ang.set(0.0);

            x_gain.set(z_gain.getDoubleValue());
            x_damp.set(z_damp.getDoubleValue());

            y_gain.set(z_gain.getDoubleValue());
            y_damp.set(z_damp.getDoubleValue());

            walk_state.set(DOUBLE_SUPPORT);
            pod_thresh_up.set(10.0);
            pod_thresh_down.set(10.0);

            // x_off.val = q_x.val;
            // y_off.val = q_y.val;
            // yaw_off.val = q_yaw.val;

            t.set(0.0);
            switch_time.set(0.0);

            break;


         case DANCE :    /* 400 */

            // dance_ab();

            break;


         case ZIGZAG :    /* 600 */

         // qd_d.yaw.val = zig_amp.val*Math.sin(2.0*PI*t.val*zig_freq.val);

      }


      if ((vel_mag.getDoubleValue()) > 0.5)
         vel_mag.set(0.5);    /* velocity limitation */
      if ((vel_mag.getDoubleValue()) < -0.5)
         vel_mag.set(-0.5);

      // qd_d_x.val = cos(forward_angle.val) * cos(q_yaw.val + vel_ang.val) * vel_mag.val;
      // qd_d_y.val = Math.sin(q_yaw.val + vel_ang.val) * vel_mag.val;

      vel_d_x.set(Math.cos(vel_ang.getDoubleValue()) * vel_mag.getDoubleValue());
      vel_d_y.set(Math.sin(vel_ang.getDoubleValue()) * vel_mag.getDoubleValue());

      vel_x.set((Math.cos(q_yaw.getDoubleValue()) * qd_x.getDoubleValue()) + (Math.sin(q_yaw.getDoubleValue()) * qd_y.getDoubleValue()));
      vel_y.set((-Math.sin(q_yaw.getDoubleValue()) * qd_x.getDoubleValue()) + (Math.cos(q_yaw.getDoubleValue()) * qd_y.getDoubleValue()));

      if (hug_parallel.getDoubleValue() > 0.5)
         q_d_pitch.set(-forward_angle.getDoubleValue());

      if (state.getDoubleValue() != DANCE)
         bug_walk();

      tau_hip1_z.set(ff_hip1_z.getDoubleValue());

      tau_hip1_x.set(ff_hip1_x.getDoubleValue());
      tau_hip1_z.set(ff_hip1_z.getDoubleValue());
      tau_knee1.set(ff_knee1.getDoubleValue());
      tau_hip2_x.set(ff_hip2_x.getDoubleValue());
      tau_hip2_z.set(ff_hip2_z.getDoubleValue());
      tau_knee2.set(ff_knee2.getDoubleValue());
      tau_hip3_x.set(ff_hip3_x.getDoubleValue());
      tau_hip3_z.set(ff_hip3_z.getDoubleValue());
      tau_knee3.set(ff_knee3.getDoubleValue());
      tau_hip4_x.set(ff_hip4_x.getDoubleValue());
      tau_hip4_z.set(ff_hip4_z.getDoubleValue());
      tau_knee4.set(ff_knee4.getDoubleValue());
      tau_hip5_x.set(ff_hip5_x.getDoubleValue());
      tau_hip5_z.set(ff_hip5_z.getDoubleValue());
      tau_knee5.set(ff_knee5.getDoubleValue());
      tau_hip6_x.set(ff_hip6_x.getDoubleValue());
      tau_hip6_z.set(ff_hip6_z.getDoubleValue());
      tau_knee6.set(ff_knee6.getDoubleValue());

   }

   public void bug_walk()
   {
      switch ((int) (walk_state.getDoubleValue()))
      {
         case DOUBLE_SUPPORT :    /* 0 */

            /* Supported by both tripods */


            /** Calculate tripod stuff */

            tripod_a_kin();
            tripod_b_kin();

            e_ramp.set(podb_mag.getDoubleValue() / (poda_mag.getDoubleValue() + podb_mag.getDoubleValue()));

            /* e_ramp assumes that the center of mass is between the two pods */

            move_body_ab();

            if (Math.sqrt(qd_x.getDoubleValue() * qd_x.getDoubleValue() + qd_y.getDoubleValue() * qd_y.getDoubleValue() + qd_yaw.getDoubleValue() * qd_yaw.getDoubleValue()) > .003)
            {
               if ((podb_h.getDoubleValue() < -pod_thresh_up.getDoubleValue()) && (t.getDoubleValue() > switch_time.getDoubleValue() + double_time.getDoubleValue()))
               {
                  walk_state.set(A_SUPPORT_B_LIFT);
                  switch_time.set(t.getDoubleValue());
               }

               if ((poda_h.getDoubleValue() < -pod_thresh_up.getDoubleValue()) && (t.getDoubleValue() > switch_time.getDoubleValue() + double_time.getDoubleValue()))
               {
                  walk_state.set(B_SUPPORT_A_LIFT);
                  switch_time.set(t.getDoubleValue());
               }
            }

            break;



         case A_SUPPORT_B_LIFT :    /* 10 */

            /* Lift tripod B */

            tripod_a_kin();
            tripod_b_kin();

            tripod_zb_d.set(lift_up.getDoubleValue() + ground_height.getDoubleValue() - q_z.getDoubleValue());
            dfoot_z_d.set(0.0);
            Kz_leg.set(75.0);
            Bz_leg.set(5.0);

            tripod_xb_d.set(podb_x.getDoubleValue());
            tripod_yb_d.set(podb_y.getDoubleValue());

            move_body_a();
            move_tripod_b();

            /* check to see if pod is off the ground, i.e. ground switches off */
            if ((gc_foot4_fs.getDoubleValue() == 0.0) && (gc_foot5_fs.getDoubleValue() == 0.0) && (gc_foot6_fs.getDoubleValue() == 0.0))
               walk_state.set(A_SUPPORT_B_SWING);

            break;




         case A_SUPPORT_B_SWING :    /* 20 */

            /* Swing Tripod B */

            tripod_a_kin();
            tripod_b_kin();

            tripod_xb_d.set(alpha.getDoubleValue() * (-proja_h_x.getDoubleValue()) + beta.getDoubleValue() * hb_hat_x.getDoubleValue());
            tripod_yb_d.set(alpha.getDoubleValue() * (-proja_h_y.getDoubleValue()) + beta.getDoubleValue() * hb_hat_y.getDoubleValue());
            tripod_thetab_d.set(alpha.getDoubleValue() * (-proja_h_theta.getDoubleValue()) + beta.getDoubleValue() * hb_hat_theta.getDoubleValue());

            tripod_zb_d.set(lift_up.getDoubleValue() + ground_height.getDoubleValue() - q_z.getDoubleValue());
            dfoot_z_d.set(0.0);
            Kz_leg.set(75.0);
            Bz_leg.set(5.0);

            move_body_a();
            move_tripod_b();


            if ((podb_h.getDoubleValue() > pod_thresh_down.getDoubleValue()) &&    /* Changed HERE */
                    (t.getDoubleValue() > switch_time.getDoubleValue() + swing_time.getDoubleValue()))
               walk_state.set(A_SUPPORT_B_DOWN);

            break;



         case A_SUPPORT_B_DOWN :    /* 30 */

            /* Put tripod B down */

            tripod_a_kin();
            tripod_b_kin();


            if (gc_foot4_fs.getDoubleValue() == 0.0)
               dfoot_z4_d.set(-2.5);
            else
               dfoot_z4_d.set(0.0);

            if (gc_foot5_fs.getDoubleValue() == 0.0)
               dfoot_z5_d.set(-2.5);
            else
               dfoot_z5_d.set(0.0);

            if (gc_foot6_fs.getDoubleValue() == 0.0)
               dfoot_z6_d.set(-2.5);
            else
               dfoot_z6_d.set(0.0);


            Kz_leg.set(0.0);
            Bz_leg.set(2.5);

            /* tripod_zb_d.set(-q_z.getDoubleValue() - .01); */

            move_body_a();
            move_tripod_b();

            if ((gc_foot4_fs.getDoubleValue() != 0.0) && (gc_foot5_fs.getDoubleValue() != 0.0) && (gc_foot6_fs.getDoubleValue() != 0.0))
            {
               walk_state.set(DOUBLE_SUPPORT);
               switch_time.set(t.getDoubleValue());
            }

            break;




         case B_SUPPORT_A_LIFT :    /* 40 */

            /* Lift tripod A */

            tripod_a_kin();
            tripod_b_kin();

            tripod_za_d.set(lift_up.getDoubleValue() + ground_height.getDoubleValue() - q_z.getDoubleValue());
            dfoot_z_d.set(0.0);
            Kz_leg.set(75.0);
            Bz_leg.set(5.0);

            tripod_xa_d.set(poda_x.getDoubleValue());
            tripod_ya_d.set(poda_y.getDoubleValue());

            move_body_b();
            move_tripod_a();

            /* check to see if pod is off the ground i.e. ground switches off */
            if ((gc_foot1_fs.getDoubleValue() == 0.0) && (gc_foot2_fs.getDoubleValue() == 0.0) && (gc_foot3_fs.getDoubleValue() == 0.0))
               walk_state.set(B_SUPPORT_A_SWING);

            break;


         case B_SUPPORT_A_SWING :    /* 50 */

            /* Swing Tripod A */

            tripod_a_kin();
            tripod_b_kin();

            tripod_xa_d.set(alpha.getDoubleValue() * (-projb_h_x.getDoubleValue()) + beta.getDoubleValue() * ha_hat_x.getDoubleValue());
            tripod_ya_d.set(alpha.getDoubleValue() * (-projb_h_y.getDoubleValue()) + beta.getDoubleValue() * ha_hat_y.getDoubleValue());
            tripod_thetaa_d.set(alpha.getDoubleValue() * (-projb_h_theta.getDoubleValue()) + beta.getDoubleValue() * ha_hat_theta.getDoubleValue());

            tripod_za_d.set(lift_up.getDoubleValue() + ground_height.getDoubleValue() - q_z.getDoubleValue());
            dfoot_z_d.set(0.0);
            Kz_leg.set(75.0);
            Bz_leg.set(5.0);

            move_body_b();
            move_tripod_a();


            if ((poda_h.getDoubleValue() > pod_thresh_down.getDoubleValue()) && (t.getDoubleValue() > switch_time.getDoubleValue() + swing_time.getDoubleValue()))
               walk_state.set(B_SUPPORT_A_DOWN);

            break;

         case B_SUPPORT_A_DOWN :    /* 60 */

            /* Put tripod A down */

            tripod_a_kin();
            tripod_b_kin();

            if (gc_foot1_fs.getDoubleValue() == 0.0)
               dfoot_z1_d.set(-2.5);
            else
               dfoot_z1_d.set(0.0);

            if (gc_foot2_fs.getDoubleValue() == 0.0)
               dfoot_z2_d.set(-2.5);
            else
               dfoot_z2_d.set(0.0);

            if (gc_foot3_fs.getDoubleValue() == 0.0)
               dfoot_z3_d.set(-2.5);
            else
               dfoot_z3_d.set(0.0);

            Kz_leg.set(0.0);
            Bz_leg.set(2.5);

            move_body_b();
            move_tripod_a();

            if ((gc_foot1_fs.getDoubleValue() != 0.0) && (gc_foot2_fs.getDoubleValue() != 0.0) && (gc_foot3_fs.getDoubleValue() != 0.0))
            {
               walk_state.set(DOUBLE_SUPPORT);
               switch_time.set(t.getDoubleValue());
            }

            break;


      }
   }


   public void move_body_a()
   {
      pend_x.set(AgileHexapodRobot.PEND_L * Math.sin(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()));
      pend_dx.set(AgileHexapodRobot.PEND_L * Math.cos(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()) * (qd_pend1.getDoubleValue() + qd_pitch.getDoubleValue()));

      pend_y.set(-AgileHexapodRobot.PEND_L * Math.sin(q_pend2.getDoubleValue() + q_roll.getDoubleValue()));
      pend_dy.set(-AgileHexapodRobot.PEND_L * Math.cos(q_pend2.getDoubleValue() + q_roll.getDoubleValue()) * (qd_pend2.getDoubleValue() + qd_roll.getDoubleValue()));

      /** virtual components */

      fXa.set(0.0);
      fYa.set(0.0);
      fZa.set(z_gain.getDoubleValue() * (q_d_z.getDoubleValue() / Math.cos(forward_angle.getDoubleValue()) + ground_height.getDoubleValue() - q_z.getDoubleValue()) - z_damp.getDoubleValue() * qd_z.getDoubleValue() + ffZa.getDoubleValue());

      tXa.set(roll_gain.getDoubleValue() * (q_d_roll.getDoubleValue() - q_roll.getDoubleValue()) - roll_damp.getDoubleValue() * qd_roll.getDoubleValue());
      tYa.set(pitch_gain.getDoubleValue() * (q_d_pitch.getDoubleValue() - q_pitch.getDoubleValue()) - pitch_damp.getDoubleValue() * qd_pitch.getDoubleValue());
      tZa.set(yaw_gain.getDoubleValue() * (q_d_yaw.getDoubleValue() - q_yaw.getDoubleValue()) + yaw_damp.getDoubleValue() * (qd_d_yaw.getDoubleValue() - qd_yaw.getDoubleValue()));


      virtualBodyForces.transform_forces_to_body(fXa, fYa, fZa, tXa, tYa, tZa);

      fXa.set(fXa.getDoubleValue() + x_gain.getDoubleValue() * (q_d_x.getDoubleValue() - q_x.getDoubleValue()) + x_damp.getDoubleValue() * (vel_d_x.getDoubleValue() - vel_x.getDoubleValue()) + PK1.getDoubleValue() * (-pend_x.getDoubleValue()) + PK2.getDoubleValue() * (-pend_dx.getDoubleValue()));
      fYa.set(fYa.getDoubleValue() + y_gain.getDoubleValue() * (q_d_y.getDoubleValue() - q_y.getDoubleValue()) + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue()) + PK3.getDoubleValue() * (-pend_y.getDoubleValue()) + PK4.getDoubleValue() * (-pend_dy.getDoubleValue()));

      /* fYa.set(fYa.getDoubleValue() + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue())); */


      /** Calculate Virtual Control Model Body Forces */
      virtualBodyForces.virtualForces_a(fXa.getDoubleValue(), fYa.getDoubleValue(), fZa.getDoubleValue(), tXa.getDoubleValue(), tYa.getDoubleValue(), tZa.getDoubleValue());
   }

   public void move_body_b()
   {
      pend_x.set(AgileHexapodRobot.PEND_L * Math.sin(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()));
      pend_dx.set(AgileHexapodRobot.PEND_L * Math.cos(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()) * (qd_pend1.getDoubleValue() + qd_pitch.getDoubleValue()));

      pend_y.set(-AgileHexapodRobot.PEND_L * Math.sin(q_pend2.getDoubleValue() + q_roll.getDoubleValue()));
      pend_dy.set(-AgileHexapodRobot.PEND_L * Math.cos(q_pend2.getDoubleValue() + q_roll.getDoubleValue()) * (qd_pend2.getDoubleValue() + qd_roll.getDoubleValue()));

      /** virtual components */
      fXb.set(0.0);
      fYb.set(0.0);
      fZb.set(z_gain.getDoubleValue() * (q_d_z.getDoubleValue() / Math.cos(forward_angle.getDoubleValue()) + ground_height.getDoubleValue() - q_z.getDoubleValue()) - z_damp.getDoubleValue() * qd_z.getDoubleValue() + ffZb.getDoubleValue());

      tXb.set(roll_gain.getDoubleValue() * (q_d_roll.getDoubleValue() - q_roll.getDoubleValue()) - roll_damp.getDoubleValue() * qd_roll.getDoubleValue());
      tYb.set(pitch_gain.getDoubleValue() * (q_d_pitch.getDoubleValue() - q_pitch.getDoubleValue()) - pitch_damp.getDoubleValue() * qd_pitch.getDoubleValue());
      tZb.set(yaw_gain.getDoubleValue() * (q_d_yaw.getDoubleValue() - q_yaw.getDoubleValue()) + yaw_damp.getDoubleValue() * (qd_d_yaw.getDoubleValue() - qd_yaw.getDoubleValue()));

      virtualBodyForces.transform_forces_to_body(fXb, fYb, fZb, tXb, tYb, tZb);

      fXb.set(fXb.getDoubleValue() + x_gain.getDoubleValue() * (q_d_x.getDoubleValue() - q_x.getDoubleValue()) + x_damp.getDoubleValue() * (vel_d_x.getDoubleValue() - vel_x.getDoubleValue()) + PK1.getDoubleValue() * (-pend_x.getDoubleValue()) + PK2.getDoubleValue() * (-pend_dx.getDoubleValue()));
      fYb.set(fYb.getDoubleValue() + y_gain.getDoubleValue() * (q_d_y.getDoubleValue() - q_y.getDoubleValue()) + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue()) + PK3.getDoubleValue() * (-pend_y.getDoubleValue()) + PK4.getDoubleValue() * (-pend_dy.getDoubleValue()));

      /* fYb.set(fYb.getDoubleValue() + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue())); */

      /** Calculate Virtual Control Model Body Forces */
      virtualBodyForces.virtualForces_b(fXb.getDoubleValue(), fYb.getDoubleValue(), fZb.getDoubleValue(), tXb.getDoubleValue(), tYb.getDoubleValue(), tZb.getDoubleValue());
   }


/*    ****************************************************************
   moves body while transitioning support from set of leg to the other
     **************************************************************** */

   public void move_body_ab()
   {
      pend_x.set(AgileHexapodRobot.PEND_L * Math.sin(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()));
      pend_dx.set(AgileHexapodRobot.PEND_L * Math.cos(q_pend1.getDoubleValue() + q_pitch.getDoubleValue()) * (qd_pend1.getDoubleValue() + qd_pitch.getDoubleValue()));

      pend_y.set(-AgileHexapodRobot.PEND_L * Math.sin(q_pend2.getDoubleValue() + q_roll.getDoubleValue()));
      pend_dy.set(-AgileHexapodRobot.PEND_L * Math.cos(q_pend2.getDoubleValue() + q_roll.getDoubleValue()) * (qd_pend2.getDoubleValue() + qd_roll.getDoubleValue()));

      /** virtual components */
      fX.set(0.0);
      fY.set(0.0);
      fZ.set(z_gain.getDoubleValue() * (q_d_z.getDoubleValue() / Math.cos(forward_angle.getDoubleValue()) + ground_height.getDoubleValue() - q_z.getDoubleValue()) - z_damp.getDoubleValue() * qd_z.getDoubleValue() + ffZa.getDoubleValue());

      tX.set(roll_gain.getDoubleValue() * (q_d_roll.getDoubleValue() - q_roll.getDoubleValue()) - roll_damp.getDoubleValue() * qd_roll.getDoubleValue());
      tY.set(pitch_gain.getDoubleValue() * (q_d_pitch.getDoubleValue() - q_pitch.getDoubleValue()) - pitch_damp.getDoubleValue() * qd_pitch.getDoubleValue());
      tZ.set(yaw_gain.getDoubleValue() * (q_d_yaw.getDoubleValue() - q_yaw.getDoubleValue()) + yaw_damp.getDoubleValue() * (qd_d_yaw.getDoubleValue() - qd_yaw.getDoubleValue()));

      /** Transform to Body Coords */
      virtualBodyForces.transform_forces_to_body(fX, fY, fZ, tX, tY, tZ);

      fX.set(fX.getDoubleValue() + x_gain.getDoubleValue() * (q_d_x.getDoubleValue() - q_x.getDoubleValue()) + x_damp.getDoubleValue() * (vel_d_x.getDoubleValue() - vel_x.getDoubleValue()) + PK1.getDoubleValue() * (-pend_x.getDoubleValue()) + PK2.getDoubleValue() * (-pend_dx.getDoubleValue()));
      fY.set(fY.getDoubleValue() + y_gain.getDoubleValue() * (q_d_y.getDoubleValue() - q_y.getDoubleValue()) + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue()) + PK3.getDoubleValue() * (-pend_y.getDoubleValue()) + PK4.getDoubleValue() * (-pend_dy.getDoubleValue()));

      /* fY.set(fY.getDoubleValue() + y_damp.getDoubleValue() * (vel_d_y.getDoubleValue() - vel_y.getDoubleValue())); */


      /** Split Between Two Legs */
      fXa.set(e_ramp.getDoubleValue() * fX.getDoubleValue());
      fYa.set(e_ramp.getDoubleValue() * fY.getDoubleValue());
      fZa.set(e_ramp.getDoubleValue() * fZ.getDoubleValue());
      tXa.set(e_ramp.getDoubleValue() * tX.getDoubleValue());
      tYa.set(e_ramp.getDoubleValue() * tY.getDoubleValue());
      tZa.set(e_ramp.getDoubleValue() * tZ.getDoubleValue());

      fXb.set((1.0 - e_ramp.getDoubleValue()) * fX.getDoubleValue());
      fYb.set((1.0 - e_ramp.getDoubleValue()) * fY.getDoubleValue());
      fZb.set((1.0 - e_ramp.getDoubleValue()) * fZ.getDoubleValue());
      tXb.set((1.0 - e_ramp.getDoubleValue()) * tX.getDoubleValue());
      tYb.set((1.0 - e_ramp.getDoubleValue()) * tY.getDoubleValue());
      tZb.set((1.0 - e_ramp.getDoubleValue()) * tZ.getDoubleValue());

      /** Compute Torques */
      virtualBodyForces.virtualForces_a(fXa.getDoubleValue(), fYa.getDoubleValue(), fZa.getDoubleValue(), tXa.getDoubleValue(), tYa.getDoubleValue(), tZa.getDoubleValue());
      virtualBodyForces.virtualForces_b(fXb.getDoubleValue(), fYb.getDoubleValue(), fZb.getDoubleValue(), tXb.getDoubleValue(), tYb.getDoubleValue(), tZb.getDoubleValue());


   }


   public void tripod_a_kin()
   {
      double ha_x, ha_y, ha_mag;
      double ha_theta;
      double poda_h;
      double a, b, c;

      /** Kinematics for each leg  (1-3) */
      LegKinematics.leg_kin(q_hip1_x.getDoubleValue(), q_hip1_z.getDoubleValue(), q_knee1.getDoubleValue(), AgileHexapodRobot.PX1, AgileHexapodRobot.PY1, foot_x1, foot_y1, foot_z1);
      LegKinematics.leg_kin(q_hip2_x.getDoubleValue(), q_hip2_z.getDoubleValue(), q_knee2.getDoubleValue(), AgileHexapodRobot.PX2, AgileHexapodRobot.PY2, foot_x2, foot_y2, foot_z2);
      LegKinematics.leg_kin(q_hip3_x.getDoubleValue(), q_hip3_z.getDoubleValue(), q_knee3.getDoubleValue(), AgileHexapodRobot.PX3, AgileHexapodRobot.PY3, foot_x3, foot_y3, foot_z3);

      poda_x.set((foot_x1.getDoubleValue() + foot_x2.getDoubleValue() + foot_x3.getDoubleValue()) / 3.0);    /* x-pos of pod */
      poda_y.set((2.0 * foot_y1.getDoubleValue() + foot_y2.getDoubleValue() + foot_y3.getDoubleValue()) / 4.0);    /* y-pos of pod */
      poda_theta.set((Math.atan2((foot_y2.getDoubleValue() - foot_y3.getDoubleValue()), (foot_x2.getDoubleValue() - foot_x3.getDoubleValue()))));

      /* angle btw pod and body */
      poda_mag.set(Math.sqrt(poda_x.getDoubleValue() * poda_x.getDoubleValue() + poda_y.getDoubleValue() * poda_y.getDoubleValue() + poda_theta.getDoubleValue() * poda_theta.getDoubleValue()));

      ha_x = Math.cos(q_yaw.getDoubleValue()) * qd_x.getDoubleValue() + Math.sin(q_yaw.getDoubleValue()) * qd_y.getDoubleValue();    /* dir of x-velocity vector */
      ha_y = -Math.sin(q_yaw.getDoubleValue()) * qd_x.getDoubleValue() + Math.cos(q_yaw.getDoubleValue()) * qd_y.getDoubleValue();    /* dir of y-velocity vector */
      ha_theta = qd_yaw.getDoubleValue();    /* angular velocity vector */
      ha_mag = Math.sqrt(ha_x * ha_x + ha_y * ha_y + ha_theta * ha_theta);

      ha_hat_x.set(ha_x / ha_mag);
      ha_hat_y.set(ha_y / ha_mag);
      ha_hat_theta.set(ha_theta / ha_mag);

      poda_h = (poda_x.getDoubleValue() * ha_hat_x.getDoubleValue() + poda_y.getDoubleValue() * ha_hat_y.getDoubleValue() + poda_theta.getDoubleValue() * ha_hat_theta.getDoubleValue());

      a = (poda_x.getDoubleValue() * ha_hat_x.getDoubleValue()) / x_max.getDoubleValue();
      b = (poda_y.getDoubleValue() * ha_hat_y.getDoubleValue()) / y_max.getDoubleValue();
      c = (poda_theta.getDoubleValue() * ha_hat_theta.getDoubleValue()) / theta_max.getDoubleValue();

      this.poda_h.set(a + b + c);    // JEP+++


      proja_h_x.set(poda_h * ha_hat_x.getDoubleValue());
      proja_h_y.set(poda_h * ha_hat_y.getDoubleValue());
      proja_h_theta.set(poda_h * ha_hat_theta.getDoubleValue());

   }

   public void tripod_b_kin()
   {
      double hb_x, hb_y, hb_mag;
      double hb_theta;
      double podb_h;
      double a, b, c;

      /** Kinematics for each leg (4-6) */
      LegKinematics.leg_kin(q_hip4_x.getDoubleValue(), q_hip4_z.getDoubleValue(), q_knee4.getDoubleValue(), AgileHexapodRobot.PX4, AgileHexapodRobot.PY4, foot_x4, foot_y4, foot_z4);
      LegKinematics.leg_kin(q_hip5_x.getDoubleValue(), q_hip5_z.getDoubleValue(), q_knee5.getDoubleValue(), AgileHexapodRobot.PX5, AgileHexapodRobot.PY5, foot_x5, foot_y5, foot_z5);
      LegKinematics.leg_kin(q_hip6_x.getDoubleValue(), q_hip6_z.getDoubleValue(), q_knee6.getDoubleValue(), AgileHexapodRobot.PX6, AgileHexapodRobot.PY6, foot_x6, foot_y6, foot_z6);

      podb_x.set((foot_x4.getDoubleValue() + foot_x5.getDoubleValue() + foot_x6.getDoubleValue()) / 3.0);    /* x-pos of pod */
      podb_y.set((2.0 * foot_y4.getDoubleValue() + foot_y5.getDoubleValue() + foot_y6.getDoubleValue()) / 4.0);    /* y-pos of pod */
      podb_theta.set((Math.atan2((foot_y5.getDoubleValue() - foot_y6.getDoubleValue()), (foot_x5.getDoubleValue() - foot_y6.getDoubleValue()))));

      /* angle btw pod and body */

      podb_mag.set(Math.sqrt(podb_x.getDoubleValue() * podb_x.getDoubleValue()    /* magnitude of pod vector */
                               + podb_y.getDoubleValue() * podb_y.getDoubleValue() + podb_theta.getDoubleValue() * podb_theta.getDoubleValue()));

      hb_x = Math.cos(q_yaw.getDoubleValue()) * qd_x.getDoubleValue() + Math.sin(q_yaw.getDoubleValue()) * qd_y.getDoubleValue();    /* dir of x-velocity vector */
      hb_y = -Math.sin(q_yaw.getDoubleValue()) * qd_x.getDoubleValue() + Math.cos(q_yaw.getDoubleValue()) * qd_y.getDoubleValue();    /* dir of y_velocity vector */
      hb_theta = qd_yaw.getDoubleValue();    /* angular velocity vector */
      hb_mag = Math.sqrt(hb_x * hb_x + hb_y * hb_y + hb_theta * hb_theta);

      hb_hat_x.set(hb_x / hb_mag);
      hb_hat_y.set(hb_y / hb_mag);
      hb_hat_theta.set(hb_theta / hb_mag);

      podb_h = (podb_x.getDoubleValue() * hb_hat_x.getDoubleValue() + podb_y.getDoubleValue() * hb_hat_y.getDoubleValue() + podb_theta.getDoubleValue() * hb_hat_theta.getDoubleValue());

      a = (podb_x.getDoubleValue() * hb_hat_x.getDoubleValue()) / x_max.getDoubleValue();
      b = (podb_y.getDoubleValue() * hb_hat_y.getDoubleValue()) / y_max.getDoubleValue();
      c = (podb_theta.getDoubleValue() * hb_hat_theta.getDoubleValue()) / theta_max.getDoubleValue();

      this.podb_h.set(a + b + c);    // JEP+++

      /*
       *  podb_h.set(Math.sqrt(sign(a)*a*a
       *     + sign(b)*b*b
       *     + sign(c)*c*c));
       */

      /* podb_h.set((-podb_h_pos)); */

      projb_h_x.set(podb_h * hb_hat_x.getDoubleValue());
      projb_h_y.set(podb_h * hb_hat_y.getDoubleValue());
      projb_h_theta.set(podb_h * ha_hat_theta.getDoubleValue());
   }

   /** *  ** First virtual tripod set **  * */

   public void move_tripod_a()
   {
      tripod_a_kin();


      /* Jacobian Formulation for each leg (1-3) */
      LegKinematics.jacobian(q_hip1_x.getDoubleValue(), q_hip1_z.getDoubleValue(), q_knee1.getDoubleValue(), qd_hip1_x.getDoubleValue(), qd_hip1_z.getDoubleValue(), qd_knee1.getDoubleValue(), dfoot_x1, dfoot_y1, dfoot_z1);
      LegKinematics.jacobian(q_hip2_x.getDoubleValue(), q_hip2_z.getDoubleValue(), q_knee2.getDoubleValue(), qd_hip2_x.getDoubleValue(), qd_hip2_z.getDoubleValue(), qd_knee2.getDoubleValue(), dfoot_x2, dfoot_y2, dfoot_z2);
      LegKinematics.jacobian(q_hip3_x.getDoubleValue(), q_hip3_z.getDoubleValue(), q_knee3.getDoubleValue(), qd_hip3_x.getDoubleValue(), qd_hip3_z.getDoubleValue(), qd_knee3.getDoubleValue(), dfoot_x3, dfoot_y3, dfoot_z3);


      /** Desired Foot Positions based on Tripod */
      foot_x1_d.set(tripod_xa_d.getDoubleValue() + Math.cos(tripod_thetaa_d.getDoubleValue()) * AgileHexapodRobot.PX1
                      - Math.sin(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY1 - leg_y_off.getDoubleValue()));
      foot_y1_d.set(tripod_ya_d.getDoubleValue() + Math.sin(tripod_thetaa_d.getDoubleValue()) * AgileHexapodRobot.PX1
                      + Math.cos(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY1 - leg_y_off.getDoubleValue()));
      foot_z1_d.set(tripod_za_d.getDoubleValue() + AgileHexapodRobot.PZ1);


      foot_x2_d.set(tripod_xa_d.getDoubleValue() + Math.cos(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PX2 + leg_x_off.getDoubleValue())
                      - Math.sin(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY2 + leg_y_off.getDoubleValue()));
      foot_y2_d.set(tripod_ya_d.getDoubleValue() + Math.sin(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PX2 + leg_x_off.getDoubleValue())
                      + Math.cos(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY2 + leg_y_off.getDoubleValue()));
      foot_z2_d.set(tripod_za_d.getDoubleValue() + AgileHexapodRobot.PZ2);


      foot_x3_d.set(tripod_xa_d.getDoubleValue() + Math.cos(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PX3 - leg_x_off.getDoubleValue())
                      - Math.sin(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY3 + leg_y_off.getDoubleValue()));
      foot_y3_d.set(tripod_ya_d.getDoubleValue() + Math.sin(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PX3 - leg_x_off.getDoubleValue())
                      + Math.cos(tripod_thetaa_d.getDoubleValue()) * (AgileHexapodRobot.PY3 + leg_y_off.getDoubleValue()));
      foot_z3_d.set(tripod_za_d.getDoubleValue() + AgileHexapodRobot.PZ3);


      /** Virtual Forces for each leg  (1-3) */

      /* K_leg & B_leg are spring and damping constants set in simulation */
      LegKinematics.virtual_f(foot_x1_d.getDoubleValue(), foot_y1_d.getDoubleValue(), foot_z1_d.getDoubleValue(), dfoot_z1_d.getDoubleValue(), foot_x1.getDoubleValue(), foot_y1.getDoubleValue(), foot_z1.getDoubleValue(), dfoot_x1.getDoubleValue(), dfoot_y1.getDoubleValue(),
                              dfoot_z1.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x1, Ffoot_y1, Ffoot_z1);
      LegKinematics.virtual_f(foot_x2_d.getDoubleValue(), foot_y2_d.getDoubleValue(), foot_z2_d.getDoubleValue(), dfoot_z2_d.getDoubleValue(), foot_x2.getDoubleValue(), foot_y2.getDoubleValue(), foot_z2.getDoubleValue(), dfoot_x2.getDoubleValue(), dfoot_y2.getDoubleValue(),
                              dfoot_z2.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x2, Ffoot_y2, Ffoot_z2);
      LegKinematics.virtual_f(foot_x3_d.getDoubleValue(), foot_y3_d.getDoubleValue(), foot_z3_d.getDoubleValue(), dfoot_z3_d.getDoubleValue(), foot_x3.getDoubleValue(), foot_y3.getDoubleValue(), foot_z3.getDoubleValue(), dfoot_x3.getDoubleValue(), dfoot_y3.getDoubleValue(),
                              dfoot_z3.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x3, Ffoot_y3, Ffoot_z3);


      /** Torques for joints on each leg (1-3) */
      LegKinematics.torque(q_hip1_x.getDoubleValue(), q_hip1_z.getDoubleValue(), q_knee1.getDoubleValue(), Ffoot_x1.getDoubleValue(), Ffoot_y1.getDoubleValue(), Ffoot_z1.getDoubleValue(), ff_hip1_x, ff_hip1_z, ff_knee1);
      LegKinematics.torque(q_hip2_x.getDoubleValue(), q_hip2_z.getDoubleValue(), q_knee2.getDoubleValue(), Ffoot_x2.getDoubleValue(), Ffoot_y2.getDoubleValue(), Ffoot_z2.getDoubleValue(), ff_hip2_x, ff_hip2_z, ff_knee2);
      LegKinematics.torque(q_hip3_x.getDoubleValue(), q_hip3_z.getDoubleValue(), q_knee3.getDoubleValue(), Ffoot_x3.getDoubleValue(), Ffoot_y3.getDoubleValue(), Ffoot_z3.getDoubleValue(), ff_hip3_x, ff_hip3_z, ff_knee3);
   }




   /** *  ** Second virtual tripod set **  * */

   public void move_tripod_b()

   {
      tripod_b_kin();

      /* Jacobian Formulation for each leg (4-6) */

      LegKinematics.jacobian(q_hip4_x.getDoubleValue(), q_hip4_z.getDoubleValue(), q_knee4.getDoubleValue(), qd_hip4_x.getDoubleValue(), qd_hip4_z.getDoubleValue(), qd_knee4.getDoubleValue(), dfoot_x4, dfoot_y4, dfoot_z4);
      LegKinematics.jacobian(q_hip5_x.getDoubleValue(), q_hip5_z.getDoubleValue(), q_knee5.getDoubleValue(), qd_hip5_x.getDoubleValue(), qd_hip5_z.getDoubleValue(), qd_knee5.getDoubleValue(), dfoot_x5, dfoot_y5, dfoot_z5);
      LegKinematics.jacobian(q_hip6_x.getDoubleValue(), q_hip6_z.getDoubleValue(), q_knee6.getDoubleValue(), qd_hip6_x.getDoubleValue(), qd_hip6_z.getDoubleValue(), qd_knee6.getDoubleValue(), dfoot_x6, dfoot_y6, dfoot_z6);


      /** Desired Foot Positions based on Tripod */
      foot_x4_d.set(tripod_xb_d.getDoubleValue() + Math.cos(tripod_thetab_d.getDoubleValue()) * AgileHexapodRobot.PX4
                      - Math.sin(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY4 + leg_y_off.getDoubleValue()));
      foot_y4_d.set(tripod_yb_d.getDoubleValue() + Math.sin(tripod_thetab_d.getDoubleValue()) * AgileHexapodRobot.PX4
                      + Math.cos(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY4 + leg_y_off.getDoubleValue()));
      foot_z4_d.set(tripod_zb_d.getDoubleValue() + AgileHexapodRobot.PZ4);


      foot_x5_d.set(tripod_xb_d.getDoubleValue() + Math.cos(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PX5 + leg_x_off.getDoubleValue())
                      - Math.sin(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY5 - leg_y_off.getDoubleValue()));
      foot_y5_d.set(tripod_yb_d.getDoubleValue() + Math.sin(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PX5 + leg_x_off.getDoubleValue())
                      + Math.cos(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY5 - leg_y_off.getDoubleValue()));
      foot_z5_d.set(tripod_zb_d.getDoubleValue() + AgileHexapodRobot.PZ5);


      foot_x6_d.set(tripod_xb_d.getDoubleValue() + Math.cos(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PX6 - leg_x_off.getDoubleValue())
                      - Math.sin(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY6 - leg_y_off.getDoubleValue()));
      foot_y6_d.set(tripod_yb_d.getDoubleValue() + Math.sin(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PX6 - leg_x_off.getDoubleValue())
                      + Math.cos(tripod_thetab_d.getDoubleValue()) * (AgileHexapodRobot.PY6 - leg_y_off.getDoubleValue()));
      foot_z6_d.set(tripod_zb_d.getDoubleValue() + AgileHexapodRobot.PZ6);


      /** Virtual Forces for each leg (4-6) */

      /* K_leg & B_leg are spring and damping constants set in bug_walk.c */
      LegKinematics.virtual_f(foot_x4_d.getDoubleValue(), foot_y4_d.getDoubleValue(), foot_z4_d.getDoubleValue(), dfoot_z4_d.getDoubleValue(), foot_x4.getDoubleValue(), foot_y4.getDoubleValue(), foot_z4.getDoubleValue(), dfoot_x4.getDoubleValue(), dfoot_y4.getDoubleValue(),
                              dfoot_z4.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x4, Ffoot_y4, Ffoot_z4);
      LegKinematics.virtual_f(foot_x5_d.getDoubleValue(), foot_y5_d.getDoubleValue(), foot_z5_d.getDoubleValue(), dfoot_z5_d.getDoubleValue(), foot_x5.getDoubleValue(), foot_y5.getDoubleValue(), foot_z5.getDoubleValue(), dfoot_x5.getDoubleValue(), dfoot_y5.getDoubleValue(),
                              dfoot_z5.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x5, Ffoot_y5, Ffoot_z5);
      LegKinematics.virtual_f(foot_x6_d.getDoubleValue(), foot_y6_d.getDoubleValue(), foot_z5_d.getDoubleValue(), dfoot_z6_d.getDoubleValue(), foot_x6.getDoubleValue(), foot_y6.getDoubleValue(), foot_z6.getDoubleValue(), dfoot_x6.getDoubleValue(), dfoot_y6.getDoubleValue(),
                              dfoot_z6.getDoubleValue(), K_leg.getDoubleValue(), B_leg.getDoubleValue(), Kz_leg.getDoubleValue(), Bz_leg.getDoubleValue(), Ffoot_x6, Ffoot_y6, Ffoot_z6);


      /** Torques for joints on each leg (4-6) */
      LegKinematics.torque(q_hip4_x.getDoubleValue(), q_hip4_z.getDoubleValue(), q_knee4.getDoubleValue(), Ffoot_x4.getDoubleValue(), Ffoot_y4.getDoubleValue(), Ffoot_z4.getDoubleValue(), ff_hip4_x, ff_hip4_z, ff_knee4);
      LegKinematics.torque(q_hip5_x.getDoubleValue(), q_hip5_z.getDoubleValue(), q_knee5.getDoubleValue(), Ffoot_x5.getDoubleValue(), Ffoot_y5.getDoubleValue(), Ffoot_z5.getDoubleValue(), ff_hip5_x, ff_hip5_z, ff_knee5);
      LegKinematics.torque(q_hip6_x.getDoubleValue(), q_hip6_z.getDoubleValue(), q_knee6.getDoubleValue(), Ffoot_x6.getDoubleValue(), Ffoot_y6.getDoubleValue(), Ffoot_z6.getDoubleValue(), ff_hip6_x, ff_hip6_z, ff_knee6);
   }


   public void initVars()
   {
      q_d_z.set(0.13);
      state.set(99);

      vel_ang.set(0);
      vel_mag.set(0.3);
      vel_ramp.set(0);
      vel_off.set(0);

//    yaw_ramp.val = 0;
//    yaw_off.val = 0;
      x_gain.set(0);
      z_gain.set(242);
      x_damp.set(-30);
      z_damp.set(33.9);
      y_gain.set(0);
      y_damp.set(-30);
      walk_state.set(0);
      pod_thresh_up.set(1);
      pod_thresh_down.set(0.8);

//    x_off.val = 0;
//    y_off.val = 0;
      switch_time.set(0);

//    body_angle.val = 0;

      ffZa.set(32);

      roll_gain.set(12);
      roll_damp.set(0.4);

      pitch_gain.set(8);
      pitch_damp.set(0.32);

      yaw_gain.set(0);
      yaw_damp.set(8);

      ffZb.set(32);

      K_leg.set(75);
      B_leg.set(5);

      x_max.set(0.025);
      y_max.set(0.02);
      theta_max.set(0.2);

      leg_y_off.set(0.08);
      leg_x_off.set(0.01);

      double_time.set(0.05);
      alpha.set(1.4);
      beta.set(0.016);
      swing_time.set(0.02);

      PK1.set(-629.2);
      PK2.set(-108);
      PK3.set(-629);
      PK4.set(-108);

      vel_d_x.set(0);
      vel_d_y.set(0);

//    zig_amp.val = 0;
//    zig_freq.val = 0;

      Kz_leg.set(0);
      Bz_leg.set(2.5);

//    xmin.val = 0;
//    xmax.val = 0;
//    ymin.val = 0;
//    ymax.val = 0;

      lift_up.set(0.05);    // 0.10;

      hug_parallel.set(1);
      t.set(0);

      q_hip1_z.set(-0.125652);
      qd_hip1_z.set(-0.00933798);
      q_hip1_x.set(-1.28698);
      qd_hip1_x.set(-0.00310202);
      q_knee1.set(1.57294);
      qd_knee1.set(-0.0118534);
      q_hip2_z.set(0.202392);
      qd_hip2_z.set(0.014717);
      q_hip2_x.set(1.27331);
      qd_hip2_x.set(-0.00245093);
      q_knee2.set(-1.45652);
      qd_knee2.set(-0.00855061);
      q_hip3_z.set(0.0875666);
      qd_hip3_z.set(0.0134054);
      q_hip3_x.set(1.35075);
      qd_hip3_x.set(-0.00320635);
      q_knee3.set(-1.57364);
      qd_knee3.set(-0.0101884);
      q_hip4_z.set(0.0264392);
      qd_hip4_z.set(0.0136122);
      q_hip4_x.set(1.27541);
      qd_hip4_x.set(-0.00262172);
      q_knee4.set(-1.56086);
      qd_knee4.set(-0.0110728);
      q_hip5_z.set(-0.0885685);
      qd_hip5_z.set(-0.00895961);
      q_hip5_x.set(-1.26575);
      qd_hip5_x.set(-0.00321131);
      q_knee5.set(1.45509);
      qd_knee5.set(-0.0104523);
      q_hip6_z.set(0.0445793);
      qd_hip6_z.set(-0.0114432);
      q_hip6_x.set(-1.3424);
      qd_hip6_x.set(-0.00389021);
      q_knee6.set(1.56316);
      qd_knee6.set(-0.00973365);

      q_pend1.set(0.0283643);
      qd_pend1.set(0);
      q_pend2.set(-0.0000386684);
      qd_pend2.set(0.000142937);

      q_x.set(-0.5);
      qd_x.set(0.000861826);
      q_y.set(-0.049837);
      qd_y.set(0.00135708);
      q_z.set(0.08);    // 0.118197;
      qd_z.set(0.0);

/*      q_yaw.set(-0.000467761);
      qd_yaw.set(-0.000295409);
      q_roll.set(0.000108109);
      qd_roll.set(0.0);
      q_pitch.set(-0.0283481);
      qd_pitch.set(0.0);*/

   }

   public YoRegistry getYoRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return name;
   }
   
   public void initialize()
   {      
   }

   public String getDescription()
   {
      return getName();
   }
}
