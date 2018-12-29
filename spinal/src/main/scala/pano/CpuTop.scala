
package pano

import spinal.core._
import spinal.lib._
import spinal.lib.io._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc.SizeMapping

import vexriscv.demo.MuraxApb3Timer

import scala.collection.mutable.ArrayBuffer

import cc._

case class CpuTop() extends Component {

    val io = new Bundle {

        val led_ctrl_apb        = master(Apb3(Apb3Gpio.getApb3Config()))
        val dvi_ctrl_apb        = master(Apb3(CCGpio.getApb3Config()))
        val gmii_ctrl_apb       = master(Apb3(GmiiCtrl.getApb3Config()))

        val switch_             = in(Bool)

        val test_pattern_nr             = out(UInt(4 bits))
        val test_pattern_const_color    = out(Pixel())

        val txt_buf_wr      = out(Bool)
        val txt_buf_rd      = out(Bool)
        val txt_buf_addr    = out(UInt(13 bits))
        val txt_buf_wr_data = out(Bits(8 bits))
        val txt_buf_rd_data = in(Bits(8 bits))

    }

//    val u_cpu = CpuComplex(CpuComplexConfig.default.copy(onChipRamSize = 8 kB, onChipRamHexFile = "sw/progmem.hex"))
    val u_cpu = CpuComplex(CpuComplexConfig.default)
    u_cpu.io.externalInterrupt <> False

    val apbMapping = ArrayBuffer[(Apb3, SizeMapping)]()

    apbMapping += io.led_ctrl_apb  -> (0x00000, 256 Byte)
    apbMapping += io.dvi_ctrl_apb  -> (0x00100, 256 Byte)
    apbMapping += io.gmii_ctrl_apb -> (0x10000, 4 kB)

    //============================================================
    // Timer
    //============================================================

    val u_timer = new MuraxApb3Timer()
    u_timer.io.interrupt        <> u_cpu.io.timerInterrupt
    apbMapping += u_timer.io.apb -> (0x20000, 4 kB)

    io.test_pattern_nr              := 5
    io.test_pattern_const_color.r   := 0
    io.test_pattern_const_color.g   := 0
    io.test_pattern_const_color.b   := 128

    io.txt_buf_wr                   := False
    io.txt_buf_rd                   := False
    io.txt_buf_addr                 := 0
    io.txt_buf_wr_data              := 0

    //============================================================
    // Local APB decoder
    //============================================================
    val apbDecoder = Apb3Decoder(
      master = u_cpu.io.apb,
      slaves = apbMapping
    )

}

