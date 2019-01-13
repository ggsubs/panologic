
package pano

import spinal.core._
import spartan6._

case class ChrontelIntfc(includeXClkN: Boolean = true) extends Bundle
{
    val reset_          = Bool

    val xclk_p          = Bool
    val xclk_n          = if (includeXClkN) Bool else null

    val v               = Bool
    val h               = Bool
    val de              = Bool
    val d               = Bits(12 bits)
}

class ChrontelPads(clkDomain: ClockDomain, includeXClkN: Boolean = true) extends Component {

    val io = new Bundle {
        val pads            = out(ChrontelIntfc(includeXClkN))

        val vsync           = in(Bool)
        val hsync           = in(Bool)
        val de              = in(Bool)
        val r               = in(UInt(8 bits))
        val g               = in(UInt(8 bits))
        val b               = in(UInt(8 bits))
    }

    val clk    = clkDomain.readClockWire
    val reset_ = clkDomain.readResetWire

    val vsync_p1 = Bool
    val hsync_p1 = Bool
    val de_p1    = Bool
    val r_p1     = UInt(8 bits)
    val g_p1     = UInt(8 bits)
    val b_p1     = UInt(8 bits)

    val vo_clk_area = new ClockingArea(clkDomain) {
        vsync_p1 := RegNext(io.vsync).addAttribute("keep", "true")
        hsync_p1 := RegNext(io.hsync).addAttribute("keep", "true")
        de_p1    := RegNext(io.de).addAttribute("keep", "true")
        r_p1     := RegNext(io.r).addAttribute("keep", "true")
        g_p1     := RegNext(io.g).addAttribute("keep", "true")
        b_p1     := RegNext(io.b).addAttribute("keep", "true")
    }

    io.pads.reset_ := reset_

    val clk0    = Bool
    val clk90   = Bool

    val clk180  = Bool
    val clk270  = Bool

    val u_dcm = new DCM_SP(
            clkdv_divide        = 2.0,
            clk_feedback        = "1X",
            clkfx_divide        = 1,
            clkfx_multiply      = 2,
            clkin_period        = "10.0"
        )

    val pad_reset = False

    u_dcm.io.RST        <> pad_reset
    u_dcm.io.CLKIN      <> clk
    u_dcm.io.CLKFB      <> clk0
    u_dcm.io.DSSEN      <> False

    u_dcm.io.PSCLK      <> False
    u_dcm.io.PSINCDEC   <> False
    u_dcm.io.PSEN       <> False
    u_dcm.io.PSDONE     <> False

    u_dcm.io.CLK0       <> clk0
    u_dcm.io.CLK90      <> clk90
    u_dcm.io.CLK180     <> clk180
    u_dcm.io.CLK270     <> clk270

    val u_pad_xclk_p = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
    u_pad_xclk_p.io.D0  <> True
    u_pad_xclk_p.io.D1  <> False
    u_pad_xclk_p.io.C0  <> clk90
    u_pad_xclk_p.io.C1  <> clk270
    u_pad_xclk_p.io.CE  <> True
    u_pad_xclk_p.io.R   <> pad_reset
    u_pad_xclk_p.io.S   <> False
    u_pad_xclk_p.io.Q   <> io.pads.xclk_p

    if (includeXClkN) new Area {
        val u_pad_xclk_n = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
        u_pad_xclk_n.io.D0     <> False
        u_pad_xclk_n.io.D1     <> True
        u_pad_xclk_n.io.C0     <> clk90
        u_pad_xclk_n.io.C1     <> clk270
        u_pad_xclk_n.io.CE     <> True
        u_pad_xclk_n.io.R      <> pad_reset
        u_pad_xclk_n.io.S      <> False
        u_pad_xclk_n.io.Q      <> io.pads.xclk_n
    }

    val u_pad_vsync = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
    u_pad_vsync.io.D0   <> vsync_p1
    u_pad_vsync.io.D1   <> vsync_p1
    u_pad_vsync.io.C0   <> clk0
    u_pad_vsync.io.C1   <> clk180
    u_pad_vsync.io.CE   <> True
    u_pad_vsync.io.R    <> pad_reset
    u_pad_vsync.io.S    <> False
    u_pad_vsync.io.Q    <> io.pads.v

    val u_pad_hsync = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
    u_pad_hsync.io.D0   <> hsync_p1
    u_pad_hsync.io.D1   <> hsync_p1
    u_pad_hsync.io.C0   <> clk0
    u_pad_hsync.io.C1   <> clk180
    u_pad_hsync.io.CE   <> True
    u_pad_hsync.io.R    <> pad_reset
    u_pad_hsync.io.S    <> False
    u_pad_hsync.io.Q    <> io.pads.h

    val u_pad_de = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
    u_pad_de.io.D0      <> de_p1
    u_pad_de.io.D1      <> de_p1
    u_pad_de.io.C0      <> clk0
    u_pad_de.io.C1      <> clk180
    u_pad_de.io.CE      <> True
    u_pad_de.io.R       <> pad_reset
    u_pad_de.io.S       <> False
    u_pad_de.io.Q       <> io.pads.de

    val d_p = Bits(12 bits)
    val d_n = Bits(12 bits)

    d_p := g_p1(3 downto 0) ## b_p1(7 downto 0)
    d_n := r_p1(7 downto 0) ## g_p1(7 downto 4)

    for(i <- 0 until 12) yield new Area {
        val u_pad_d = new ODDR2(ddr_alignment = "C0", srtype = "ASYNC")
        u_pad_d.io.D0   <> d_p(i)
        u_pad_d.io.D1   <> d_n(i)
        u_pad_d.io.C0   <> clk0
        u_pad_d.io.C1   <> clk180
        u_pad_d.io.CE   <> True
        u_pad_d.io.R    <> pad_reset
        u_pad_d.io.S    <> False
        u_pad_d.io.Q    <> io.pads.d(i)
    }
}
