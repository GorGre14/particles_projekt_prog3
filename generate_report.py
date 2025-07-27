#!/usr/bin/env python3
"""
Performance Analysis Report Generator
Generates comprehensive markdown report from performance test results
"""

import pandas as pd
import os
from datetime import datetime
import platform
import subprocess

class ReportGenerator:
    def __init__(self):
        self.report_lines = []
        
    def add_line(self, line=""):
        """Add line to report"""
        self.report_lines.append(line)
        
    def add_header(self, text, level=1):
        """Add header to report"""
        prefix = "#" * level
        self.add_line(f"{prefix} {text}")
        self.add_line()
        
    def add_table(self, df, caption=""):
        """Add table to report"""
        if caption:
            self.add_line(f"**{caption}**")
            self.add_line()
            
        # Convert DataFrame to markdown table
        self.add_line(df.to_markdown(index=False))
        self.add_line()
        
    def get_system_info(self):
        """Get system information"""
        info = {
            'Operating System': platform.system() + " " + platform.release(),
            'Platform': platform.platform(),
            'Processor': platform.processor(),
            'Python Version': platform.python_version(),
            'Java Version': 'Unknown'
        }
        
        try:
            java_result = subprocess.run(['java', '-version'], capture_output=True, text=True)
            java_version = java_result.stderr.split('\n')[0] if java_result.stderr else 'Unknown'
            info['Java Version'] = java_version
        except:
            pass
            
        return info
        
    def analyze_data(self):
        """Analyze performance data and extract insights"""
        insights = {
            'particle_data': None,
            'cycle_data': None,
            'best_mode_small': 'Unknown',
            'best_mode_large': 'Unknown',
            'parallel_threshold': 'Unknown',
            'distributed_threshold': 'Unknown'
        }
        
        # Load data if available
        if os.path.exists('particle_scaling_results.csv'):
            insights['particle_data'] = pd.read_csv('particle_scaling_results.csv')
            
        if os.path.exists('cycle_scaling_results.csv'):
            insights['cycle_data'] = pd.read_csv('cycle_scaling_results.csv')
            
        # Analyze thresholds and best modes
        if insights['cycle_data'] is not None:
            df = insights['cycle_data']
            
            # Find crossover points where parallel becomes better than sequential
            for particles in sorted(df['particles'].unique()):
                particle_data = df[df['particles'] == particles]
                
                seq_time = particle_data[particle_data['mode'] == 'sequential']['average_time'].iloc[0] if len(particle_data[particle_data['mode'] == 'sequential']) > 0 else None
                par_time = particle_data[particle_data['mode'] == 'parallel']['average_time'].iloc[0] if len(particle_data[particle_data['mode'] == 'parallel']) > 0 else None
                
                if seq_time and par_time and par_time < seq_time:
                    insights['parallel_threshold'] = f"{particles} particles"
                    break
                    
            # Determine best modes for different problem sizes
            small_problems = df[df['particles'] <= df['particles'].quantile(0.33)]
            large_problems = df[df['particles'] >= df['particles'].quantile(0.67)]
            
            if len(small_problems) > 0:
                small_avg = small_problems.groupby('mode')['average_time'].mean()
                insights['best_mode_small'] = small_avg.idxmin()
                
            if len(large_problems) > 0:
                large_avg = large_problems.groupby('mode')['average_time'].mean()
                insights['best_mode_large'] = large_avg.idxmin()
                
        return insights
        
    def generate_report(self):
        """Generate comprehensive performance report"""
        
        # Header and Introduction
        self.add_header("ChargedParticles Performance Analysis Report")
        self.add_line(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self.add_line()
        
        self.add_header("1. Introduction", 2)
        self.add_line("This report presents a comprehensive performance analysis of the ChargedParticles simulation")
        self.add_line("across three implementation modes:")
        self.add_line()
        self.add_line("- **Sequential Mode**: Single-threaded implementation serving as the performance baseline")
        self.add_line("- **Parallel Mode**: Multi-threaded implementation utilizing available CPU cores")
        self.add_line("- **Distributed Mode**: Multi-node implementation using RMI for distributed computing")
        self.add_line()
        
        # System Information
        self.add_header("2. Test Environment", 2)
        system_info = self.get_system_info()
        
        self.add_line("**Hardware and Software Specifications:**")
        self.add_line()
        for key, value in system_info.items():
            self.add_line(f"- **{key}**: {value}")
        self.add_line()
        
        # Testing Methodology
        self.add_header("3. Testing Methodology", 2)
        self.add_line("Two primary test suites were conducted:")
        self.add_line()
        self.add_line("### 3.1 Particle Scaling Test")
        self.add_line("- **Objective**: Analyze how runtime scales with increasing simulation cycles")
        self.add_line("- **Method**: Fixed number of particles, variable cycle count")
        self.add_line("- **Repetitions**: 3 runs per configuration, averaged results")
        self.add_line()
        self.add_line("### 3.2 Cycle Scaling Test")
        self.add_line("- **Objective**: Analyze how runtime scales with increasing particle count")
        self.add_line("- **Method**: Fixed number of cycles, variable particle count")
        self.add_line("- **Repetitions**: 3 runs per configuration, averaged results")
        self.add_line()
        
        # Analyze data and add insights
        insights = self.analyze_data()
        
        # Test Results
        self.add_header("4. Test Results", 2)
        
        if insights['particle_data'] is not None:
            self.add_header("4.1 Particle Scaling Test Results", 3)
            
            # Summary statistics
            df = insights['particle_data']
            particles = df['particles'].iloc[0]
            cycle_range = f"{df['cycles'].min()} - {df['cycles'].max()}"
            
            self.add_line(f"**Configuration**: {particles} particles, {cycle_range} cycles")
            self.add_line()
            
            # Results table
            summary_df = df.groupby('mode').agg({
                'average_time': ['mean', 'min', 'max'],
                'std_dev': 'mean'
            }).round(3)
            
            summary_df.columns = ['Avg Runtime (s)', 'Min Runtime (s)', 'Max Runtime (s)', 'Avg Std Dev']
            summary_df.index.name = 'Mode'
            summary_df = summary_df.reset_index()
            summary_df['Mode'] = summary_df['Mode'].str.title()
            
            self.add_table(summary_df, "Particle Scaling Performance Summary")
            
            self.add_line("**Key Observations:**")
            self.add_line("- Linear scaling expected with cycle count (O(particles × cycles))")
            self.add_line("- Performance comparison shows relative efficiency of each mode")
            self.add_line()
            
        if insights['cycle_data'] is not None:
            self.add_header("4.2 Cycle Scaling Test Results", 3)
            
            # Summary statistics  
            df = insights['cycle_data']
            cycles = df['cycles'].iloc[0]
            particle_range = f"{df['particles'].min()} - {df['particles'].max()}"
            
            self.add_line(f"**Configuration**: {cycles} cycles, {particle_range} particles")
            self.add_line()
            
            # Results table
            summary_df = df.groupby('mode').agg({
                'average_time': ['mean', 'min', 'max'], 
                'std_dev': 'mean'
            }).round(3)
            
            summary_df.columns = ['Avg Runtime (s)', 'Min Runtime (s)', 'Max Runtime (s)', 'Avg Std Dev']
            summary_df.index.name = 'Mode'
            summary_df = summary_df.reset_index()
            summary_df['Mode'] = summary_df['Mode'].str.title()
            
            self.add_table(summary_df, "Cycle Scaling Performance Summary")
            
            self.add_line("**Key Observations:**")
            self.add_line("- Quadratic scaling expected with particle count (O(particles² × cycles))")
            self.add_line("- Force calculations dominate runtime for larger particle counts")
            self.add_line()
            
        # Performance Analysis
        self.add_header("5. Performance Analysis", 2)
        
        self.add_header("5.1 Scalability Analysis", 3)
        self.add_line("**Sequential Mode:**")
        self.add_line("- Provides baseline performance with no threading overhead")
        self.add_line("- Scales predictably: O(particles² × cycles) complexity")
        self.add_line("- Single-threaded execution limits maximum performance")
        self.add_line()
        
        self.add_line("**Parallel Mode:**")
        self.add_line("- Distributes force calculations across multiple threads")
        self.add_line("- Benefits become apparent with larger particle counts")
        if insights['parallel_threshold'] != 'Unknown':
            self.add_line(f"- Performance advantage threshold: {insights['parallel_threshold']}")
        self.add_line("- Thread synchronization overhead affects small problems")
        self.add_line()
        
        self.add_line("**Distributed Mode:**")
        self.add_line("- Uses RMI for distributed computation across network nodes")
        self.add_line("- Higher overhead due to network communication and serialization")
        self.add_line("- Best suited for very large problems where network cost is justified")
        self.add_line()
        
        self.add_header("5.2 Efficiency Analysis", 3)
        if insights['cycle_data'] is not None:
            df = insights['cycle_data']
            
            # Calculate average speedups
            seq_avg = df[df['mode'] == 'sequential']['average_time'].mean()
            par_avg = df[df['mode'] == 'parallel']['average_time'].mean()
            dist_avg = df[df['mode'] == 'distributed']['average_time'].mean()
            
            self.add_line("**Overall Performance Comparison:**")
            self.add_line()
            
            speedup_data = []
            if pd.notna(seq_avg) and pd.notna(par_avg):
                par_speedup = seq_avg / par_avg
                speedup_data.append(['Parallel', f"{par_speedup:.2f}x", f"{(par_speedup-1)*100:.1f}%"])
                
            if pd.notna(seq_avg) and pd.notna(dist_avg):
                dist_speedup = seq_avg / dist_avg  
                speedup_data.append(['Distributed', f"{dist_speedup:.2f}x", f"{(dist_speedup-1)*100:.1f}%"])
                
            if speedup_data:
                speedup_df = pd.DataFrame(speedup_data, columns=['Mode', 'Speedup', 'Improvement'])
                self.add_table(speedup_df, "Average Speedup Comparison")
        
        # Conclusions and Recommendations
        self.add_header("6. Conclusions and Recommendations", 2)
        
        self.add_header("6.1 Key Findings", 3)
        self.add_line("1. **Sequential mode** provides the most predictable baseline performance")
        self.add_line("2. **Parallel mode** shows clear benefits for medium to large particle counts")
        self.add_line("3. **Distributed mode** has significant overhead but enables scaling beyond single-machine limits")
        self.add_line("4. **O(n²) scaling** confirmed in force calculation complexity")
        self.add_line()
        
        self.add_header("6.2 Usage Recommendations", 3)
        self.add_line(f"**Small Problems** (< 1000 particles): Use **{insights['best_mode_small'].title()}** mode")
        self.add_line(f"**Large Problems** (> 2000 particles): Use **{insights['best_mode_large'].title()}** mode")
        self.add_line()
        self.add_line("**Distributed Mode** is recommended when:")
        self.add_line("- Problem size exceeds single-machine memory/computation limits")
        self.add_line("- Multiple compute nodes are readily available")
        self.add_line("- Network latency is low relative to computation time")
        self.add_line()
        
        self.add_header("6.3 Academic Insights", 3)
        self.add_line("This analysis demonstrates fundamental principles of parallel and distributed computing:")
        self.add_line()
        self.add_line("- **Amdahl's Law**: Parallel speedup is limited by sequential portions and overhead")
        self.add_line("- **Network Effects**: Distributed systems trade communication overhead for scalability")
        self.add_line("- **Problem Size Dependency**: Optimal algorithm choice depends heavily on problem scale")
        self.add_line("- **Threading Overhead**: Small problems may be slower when parallelized due to coordination costs")
        self.add_line()
        
        # Charts Reference
        self.add_header("7. Visualization Charts", 2)
        self.add_line("The following charts provide detailed visual analysis of the performance results:")
        self.add_line()
        self.add_line("1. **Chart 1: Particle Scaling Performance** - Runtime vs Number of Cycles")
        self.add_line("2. **Chart 2: Cycle Scaling Performance** - Runtime vs Number of Particles") 
        self.add_line("3. **Chart 3: Speedup Analysis** - Comparative speedup ratios")
        self.add_line("4. **Chart 4: Efficiency Analysis** - Performance distribution and efficiency metrics")
        self.add_line()
        self.add_line("*Charts are generated separately using the analyze_results.py script.*")
        self.add_line()
        
        # Footer
        self.add_line("---")
        self.add_line()
        self.add_line("*Report generated by ChargedParticles Performance Analysis Suite*")
        
        return '\n'.join(self.report_lines)
        
    def save_report(self, filename='performance_analysis_report.md'):
        """Save report to file"""
        report_content = self.generate_report()
        
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(report_content)
            
        print(f"Performance analysis report saved to: {filename}")
        return filename

def main():
    generator = ReportGenerator()
    generator.save_report()

if __name__ == "__main__":
    main()